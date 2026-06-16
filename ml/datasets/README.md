# Notification Training Dataset

Place training data here when you are ready to train the production classifier.

Supported formats:

- CSV: `.csv`
- JSON Lines: `.jsonl`

Required columns/keys:

- `label`: one of `urgent`, `important`, `low`
- `body`: notification body text

Optional columns/keys:

- `title`
- `sender`
- `packageName`

Example CSV:

```csv
label,title,body,sender,packageName
urgent,Security code,Your OTP is 123456 and expires in 5 minutes,Bank,com.bank.app
important,Calendar,Team sync starts in 30 minutes,Calendar,com.calendar
low,Sale,40% off today only,Shop,com.store
```

Keep private user notification exports out of git. Use consented, anonymized, or public data only.
