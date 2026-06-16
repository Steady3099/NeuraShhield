# Trained Model Outputs

The training script writes model artifacts here by default:

- `notification_classifier.keras`: Keras training checkpoint
- `notification_classifier_dynamic.tflite`: dynamic-range quantized TFLite model
- `notification_classifier_float16.tflite`: float16-quantized TFLite model
- `notification_classifier_metadata.json`: tokenizer/model metadata
- `notification_classifier_report.json`: metrics and confusion matrix

The Android app loads:

```text
app/src/main/assets/notification_classifier.tflite
```

Use the training script with `--install-asset` to copy the selected optimized model into the app asset path.
