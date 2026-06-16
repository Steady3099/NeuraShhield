#!/usr/bin/env python3
"""Train and export NeuraShhield's on-device notification classifier.

The Android app uses hashed token IDs instead of a stored vocabulary. This keeps
the asset small, avoids shipping notification-derived vocabularies, and makes
the training/inference contract stable.
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import random
import re
import shutil
from collections import Counter
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

import numpy as np
import tensorflow as tf


LABELS = ["urgent", "important", "low"]
LABEL_TO_ID = {label: index for index, label in enumerate(LABELS)}
MAX_TOKENS = 128
VOCAB_SIZE = 30_001
TOKEN_RE = re.compile(r"[\w$%]+", re.UNICODE)


def main() -> None:
    args = parse_args()
    seed_everything(args.seed)

    rows = load_rows(args.data)
    if len(rows) < 30:
        raise SystemExit("Need at least 30 labeled rows to train. Add a larger dataset first.")

    random.shuffle(rows)
    texts = [compose_text(row) for row in rows]
    labels = [normalize_label(row["label"]) for row in rows]

    x = np.array([tokenize(text) for text in texts], dtype=np.int32)
    y = np.array([LABEL_TO_ID[label] for label in labels], dtype=np.int32)

    train_idx, val_idx, test_idx = stratified_split(y, val_fraction=args.val_fraction, test_fraction=args.test_fraction)
    x_train, y_train = x[train_idx], y[train_idx]
    x_val, y_val = x[val_idx], y[val_idx]
    x_test, y_test = x[test_idx], y[test_idx]

    model = build_model(
        embedding_dim=args.embedding_dim,
        dropout=args.dropout,
        learning_rate=args.learning_rate,
    )
    model.summary()

    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor="val_macro_f1",
            mode="max",
            patience=args.patience,
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=max(1, args.patience // 2),
            min_lr=1e-5,
        ),
    ]

    model.fit(
        x_train,
        y_train,
        validation_data=(x_val, y_val),
        epochs=args.epochs,
        batch_size=args.batch_size,
        class_weight=class_weights(y_train),
        callbacks=callbacks,
        verbose=2,
    )

    args.output_dir.mkdir(parents=True, exist_ok=True)
    keras_path = args.output_dir / "notification_classifier.keras"
    model.save(keras_path)

    report = evaluate(model, x_test, y_test)
    metadata = {
        "labels": LABELS,
        "label_order": "[urgent, important, low]",
        "max_tokens": MAX_TOKENS,
        "vocab_size": VOCAB_SIZE,
        "token_regex": TOKEN_RE.pattern,
        "hash_algorithm": "Kotlin Int overflow hash: hash = 31 * hash + char.code; token = (hash & Int.MAX_VALUE) % 30000 + 1",
        "input": {"name": "token_ids", "dtype": "int32", "shape": [1, MAX_TOKENS]},
        "output": {"name": "tier_probabilities", "dtype": "float32", "shape": [1, 3]},
        "training_rows": len(rows),
        "class_counts": Counter(labels),
        "metrics": report,
    }

    dynamic_path = args.output_dir / "notification_classifier_dynamic.tflite"
    export_tflite(model, dynamic_path, quantization="dynamic")

    float16_path = args.output_dir / "notification_classifier_float16.tflite"
    export_tflite(model, float16_path, quantization="float16")

    report_path = args.output_dir / "notification_classifier_report.json"
    metadata_path = args.output_dir / "notification_classifier_metadata.json"
    write_json(report_path, report)
    write_json(metadata_path, metadata)

    selected = dynamic_path if args.asset_variant == "dynamic" else float16_path
    if args.install_asset:
        args.asset_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(selected, args.asset_path)
        print(f"Installed {selected} -> {args.asset_path}")

    print(f"Wrote: {keras_path}")
    print(f"Wrote: {dynamic_path}")
    print(f"Wrote: {float16_path}")
    print(f"Wrote: {report_path}")
    print(f"Wrote: {metadata_path}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, required=True, help="CSV or JSONL dataset path.")
    parser.add_argument("--output-dir", type=Path, default=Path("ml/models"))
    parser.add_argument("--asset-path", type=Path, default=Path("app/src/main/assets/notification_classifier.tflite"))
    parser.add_argument("--asset-variant", choices=["dynamic", "float16"], default="dynamic")
    parser.add_argument("--install-asset", action="store_true")
    parser.add_argument("--epochs", type=int, default=12)
    parser.add_argument("--batch-size", type=int, default=256)
    parser.add_argument("--embedding-dim", type=int, default=48)
    parser.add_argument("--dropout", type=float, default=0.18)
    parser.add_argument("--learning-rate", type=float, default=2e-3)
    parser.add_argument("--patience", type=int, default=3)
    parser.add_argument("--val-fraction", type=float, default=0.1)
    parser.add_argument("--test-fraction", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=42)
    return parser.parse_args()


def load_rows(path: Path) -> List[Dict[str, str]]:
    if not path.exists():
        raise SystemExit(f"Dataset not found: {path}")
    if path.suffix.lower() == ".csv":
        with path.open(newline="", encoding="utf-8") as handle:
            return [normalize_row(row) for row in csv.DictReader(handle)]
    if path.suffix.lower() == ".jsonl":
        with path.open(encoding="utf-8") as handle:
            return [normalize_row(json.loads(line)) for line in handle if line.strip()]
    raise SystemExit("Unsupported dataset format. Use .csv or .jsonl")


def normalize_row(row: Dict[str, object]) -> Dict[str, str]:
    normalized = {str(key): "" if value is None else str(value) for key, value in row.items()}
    if "label" not in normalized or "body" not in normalized:
        raise SystemExit("Every row must include at least label and body.")
    normalized["label"] = normalize_label(normalized["label"])
    return normalized


def normalize_label(value: str) -> str:
    normalized = value.strip().lower()
    if normalized not in LABEL_TO_ID:
        raise SystemExit(f"Unsupported label {value!r}. Use one of: {', '.join(LABELS)}")
    return normalized


def compose_text(row: Dict[str, str]) -> str:
    return " ".join(
        part.strip()
        for part in [
            row.get("title", ""),
            row.get("sender", ""),
            row.get("packageName", ""),
            row.get("body", ""),
        ]
        if part and part.strip()
    )


def tokenize(text: str, max_tokens: int = MAX_TOKENS) -> np.ndarray:
    tokens = [stable_token_id(match.group(0).lower()) for match in TOKEN_RE.finditer(text)]
    padded = tokens[:max_tokens] + [0] * max(0, max_tokens - len(tokens))
    return np.array(padded, dtype=np.int32)


def stable_token_id(token: str) -> int:
    value = 7
    for char in token:
        value = to_kotlin_int(31 * value + ord(char))
    return (value & 0x7FFFFFFF) % 30_000 + 1


def to_kotlin_int(value: int) -> int:
    value &= 0xFFFFFFFF
    return value - 0x100000000 if value & 0x80000000 else value


def build_model(embedding_dim: int, dropout: float, learning_rate: float) -> tf.keras.Model:
    token_ids = tf.keras.Input(shape=(MAX_TOKENS,), dtype=tf.int32, name="token_ids")
    x = tf.keras.layers.Embedding(VOCAB_SIZE, embedding_dim, mask_zero=True, name="hashed_embedding")(token_ids)
    x = tf.keras.layers.SpatialDropout1D(dropout)(x)

    conv3 = tf.keras.layers.SeparableConv1D(64, 3, padding="same", activation="relu")(x)
    conv5 = tf.keras.layers.SeparableConv1D(64, 5, padding="same", activation="relu")(x)
    pooled = tf.keras.layers.Concatenate()(
        [
            tf.keras.layers.GlobalMaxPooling1D()(conv3),
            tf.keras.layers.GlobalAveragePooling1D()(conv3),
            tf.keras.layers.GlobalMaxPooling1D()(conv5),
            tf.keras.layers.GlobalAveragePooling1D()(conv5),
        ]
    )
    x = tf.keras.layers.Dropout(dropout)(pooled)
    x = tf.keras.layers.Dense(96, activation="relu")(x)
    x = tf.keras.layers.Dropout(dropout)(x)
    probabilities = tf.keras.layers.Dense(3, activation="softmax", name="tier_probabilities")(x)

    model = tf.keras.Model(token_ids, probabilities)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss="sparse_categorical_crossentropy",
        metrics=[
            "accuracy",
            MacroF1(name="macro_f1"),
        ],
    )
    return model


class MacroF1(tf.keras.metrics.Metric):
    def __init__(self, name: str = "macro_f1", **kwargs) -> None:
        super().__init__(name=name, **kwargs)
        self.confusion = self.add_weight(
            name="confusion",
            shape=(len(LABELS), len(LABELS)),
            initializer="zeros",
        )

    def update_state(self, y_true, y_pred, sample_weight=None) -> None:
        labels = tf.cast(tf.reshape(y_true, [-1]), tf.int32)
        predictions = tf.argmax(y_pred, axis=-1, output_type=tf.int32)
        matrix = tf.math.confusion_matrix(labels, predictions, num_classes=len(LABELS), dtype=self.dtype)
        self.confusion.assign_add(matrix)

    def result(self):
        true_positive = tf.linalg.diag_part(self.confusion)
        predicted = tf.reduce_sum(self.confusion, axis=0)
        actual = tf.reduce_sum(self.confusion, axis=1)
        precision = tf.math.divide_no_nan(true_positive, predicted)
        recall = tf.math.divide_no_nan(true_positive, actual)
        f1 = tf.math.divide_no_nan(2 * precision * recall, precision + recall)
        return tf.reduce_mean(f1)

    def reset_state(self) -> None:
        self.confusion.assign(tf.zeros_like(self.confusion))


def class_weights(labels: np.ndarray) -> Dict[int, float]:
    counts = Counter(labels.tolist())
    total = len(labels)
    return {
        class_id: total / (len(LABELS) * max(1, counts.get(class_id, 0)))
        for class_id in range(len(LABELS))
    }


def stratified_split(labels: np.ndarray, val_fraction: float, test_fraction: float) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    train: List[int] = []
    val: List[int] = []
    test: List[int] = []
    for label in sorted(set(labels.tolist())):
        indices = np.where(labels == label)[0].tolist()
        random.shuffle(indices)
        test_count = max(1, math.floor(len(indices) * test_fraction))
        val_count = max(1, math.floor(len(indices) * val_fraction))
        test.extend(indices[:test_count])
        val.extend(indices[test_count : test_count + val_count])
        train.extend(indices[test_count + val_count :])
    return np.array(train), np.array(val), np.array(test)


def evaluate(model: tf.keras.Model, x_test: np.ndarray, y_test: np.ndarray) -> Dict[str, object]:
    probabilities = model.predict(x_test, batch_size=512, verbose=0)
    predictions = np.argmax(probabilities, axis=1)
    confusion = confusion_matrix(y_test, predictions)
    per_class = {}
    f1_values = []
    for index, label in enumerate(LABELS):
        tp = confusion[index][index]
        fp = sum(confusion[row][index] for row in range(len(LABELS)) if row != index)
        fn = sum(confusion[index][col] for col in range(len(LABELS)) if col != index)
        precision = safe_div(tp, tp + fp)
        recall = safe_div(tp, tp + fn)
        f1 = safe_div(2 * precision * recall, precision + recall)
        f1_values.append(f1)
        per_class[label] = {"precision": precision, "recall": recall, "f1": f1}

    return {
        "accuracy": float(np.mean(predictions == y_test)),
        "macro_f1": float(np.mean(f1_values)),
        "per_class": per_class,
        "confusion_matrix": confusion,
    }


def confusion_matrix(actual: Sequence[int], predicted: Sequence[int]) -> List[List[int]]:
    matrix = [[0 for _ in LABELS] for _ in LABELS]
    for truth, guess in zip(actual, predicted):
        matrix[int(truth)][int(guess)] += 1
    return matrix


def safe_div(numerator: float, denominator: float) -> float:
    return float(numerator / denominator) if denominator else 0.0


def export_tflite(model: tf.keras.Model, path: Path, quantization: str) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    if quantization == "dynamic":
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    elif quantization == "float16":
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
    else:
        raise ValueError(f"Unsupported quantization mode: {quantization}")
    path.write_bytes(converter.convert())


def write_json(path: Path, value: object) -> None:
    def default(obj):
        if isinstance(obj, Counter):
            return dict(obj)
        if isinstance(obj, np.integer):
            return int(obj)
        if isinstance(obj, np.floating):
            return float(obj)
        raise TypeError(f"Cannot serialize {type(obj)!r}")

    path.write_text(json.dumps(value, indent=2, sort_keys=True, default=default), encoding="utf-8")


def seed_everything(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    tf.random.set_seed(seed)


if __name__ == "__main__":
    main()
