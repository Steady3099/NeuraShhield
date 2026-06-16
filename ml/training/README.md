# NeuraShhield Notification Classifier Training

This package trains the on-device notification tier classifier used by the Android app.

The model is intentionally small:

- Input: `int32[1,128]` hashed token IDs
- Output: `float[3]` probabilities in this order: `urgent`, `important`, `low`
- Architecture: hashed token embedding + lightweight convolution/pooling head
- Export: optimized TensorFlow Lite models

The training tokenizer mirrors `NotificationTokenizer.kt`, so the model sees the same token IDs during Android inference that it saw during training.

## Dataset

Use CSV or JSONL with at least:

```text
label,body
```

Recommended:

```text
label,title,body,sender,packageName
```

Labels must be:

```text
urgent
important
low
```

## Train

```bash
python3 -m pip install -r ml/training/requirements.txt
python3 ml/training/train_notification_classifier.py \
  --data ml/datasets/notifications.csv \
  --epochs 12 \
  --batch-size 256 \
  --install-asset
```

Outputs are written to `ml/models/`. With `--install-asset`, the chosen optimized model is copied to:

```text
app/src/main/assets/notification_classifier.tflite
```

## Large Dataset Guidance

For a production model, aim for at least tens of thousands of labeled notifications per class, balanced across:

- banking and security
- messaging
- calendar and work tools
- ecommerce and promotions
- delivery/logistics
- social reactions
- news
- device/system alerts

Do not train on raw private notifications without consent and anonymization.
