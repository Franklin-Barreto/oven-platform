locals {
  media_bucket_name             = "oven-platform-staging-media-${data.aws_caller_identity.current.account_id}"
  media_access_logs_bucket_name = "oven-platform-staging-media-logs-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket" "media" {
  bucket        = local.media_bucket_name
  force_destroy = true

  tags = {
    Name = local.media_bucket_name
  }
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket = aws_s3_bucket.media.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

data "aws_iam_policy_document" "media_bucket" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    actions = ["s3:*"]

    resources = [
      aws_s3_bucket.media.arn,
      "${aws_s3_bucket.media.arn}/*",
    ]

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

  statement {
    sid    = "AllowCloudFrontRead"
    effect = "Allow"

    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${aws_s3_bucket.media.arn}/tenants/*/images/*",
    ]

    principals {
      type = "Service"
      identifiers = [
        "cloudfront.amazonaws.com",
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values = [
        aws_cloudfront_distribution.media.arn,
      ]
    }
  }
}

resource "aws_s3_bucket_policy" "media" {
  bucket = aws_s3_bucket.media.id
  policy = data.aws_iam_policy_document.media_bucket.json
}

resource "aws_s3_bucket_cors_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  cors_rule {
    id = "direct-image-upload"

    allowed_origins = var.media_cors_allowed_origins
    allowed_methods = ["PUT"]

    allowed_headers = [
      "content-type",
      "x-amz-checksum-sha256",
    ]

    expose_headers  = ["ETag"]
    max_age_seconds = 300
  }
}

resource "aws_s3_bucket" "media_access_logs" {
  bucket        = local.media_access_logs_bucket_name
  force_destroy = true

  tags = {
    Name = local.media_access_logs_bucket_name
  }
}

resource "aws_s3_bucket_public_access_block" "media_access_logs" {
  bucket = aws_s3_bucket.media_access_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "media_access_logs" {
  bucket = aws_s3_bucket.media_access_logs.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media_access_logs" {
  bucket = aws_s3_bucket.media_access_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "media_access_logs" {
  bucket = aws_s3_bucket.media_access_logs.id

  rule {
    id     = "expire-media-access-logs"
    status = "Enabled"

    filter {}

    expiration {
      days = 30
    }
  }
}

data "aws_iam_policy_document" "media_access_logs" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    actions = ["s3:*"]

    resources = [
      aws_s3_bucket.media_access_logs.arn,
      "${aws_s3_bucket.media_access_logs.arn}/*",
    ]

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

  statement {
    sid    = "AllowS3LogDelivery"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["logging.s3.amazonaws.com"]
    }

    actions = ["s3:PutObject"]

    resources = [
      "${aws_s3_bucket.media_access_logs.arn}/media/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [aws_s3_bucket.media.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "media_access_logs" {
  bucket = aws_s3_bucket.media_access_logs.id
  policy = data.aws_iam_policy_document.media_access_logs.json
}

resource "aws_s3_bucket_logging" "media" {
  bucket = aws_s3_bucket.media.id

  target_bucket = aws_s3_bucket.media_access_logs.id
  target_prefix = "media/"

  depends_on = [
    aws_s3_bucket_policy.media_access_logs,
  ]
}
