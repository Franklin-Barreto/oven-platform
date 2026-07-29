locals {
  staging_media_bucket_arn             = "arn:${data.aws_partition.current.partition}:s3:::oven-platform-staging-media-${data.aws_caller_identity.current.account_id}"
  staging_media_logs_bucket_arn        = "arn:${data.aws_partition.current.partition}:s3:::oven-platform-staging-media-logs-${data.aws_caller_identity.current.account_id}"
  staging_media_provisioner_policy_arn = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:policy/oven-platform-terraform-staging-media"
  staging_media_distribution_arn       = "arn:${data.aws_partition.current.partition}:cloudfront::${data.aws_caller_identity.current.account_id}:distribution/*"
}

data "aws_iam_policy_document" "staging_media_provisioner" {
  statement {
    sid    = "ManageStagingMediaBuckets"
    effect = "Allow"

    actions = [
      "s3:CreateBucket",
      "s3:DeleteBucket",
      "s3:DeleteBucketCORS",
      "s3:DeleteBucketOwnershipControls",
      "s3:DeleteBucketPolicy",
      "s3:DeleteBucketPublicAccessBlock",
      "s3:GetAccelerateConfiguration",
      "s3:GetBucketAcl",
      "s3:GetBucketCORS",
      "s3:GetBucketLocation",
      "s3:GetBucketLogging",
      "s3:GetBucketObjectLockConfiguration",
      "s3:GetBucketOwnershipControls",
      "s3:GetBucketPolicy",
      "s3:GetBucketPublicAccessBlock",
      "s3:GetBucketRequestPayment",
      "s3:GetBucketTagging",
      "s3:GetBucketVersioning",
      "s3:GetBucketWebsite",
      "s3:GetEncryptionConfiguration",
      "s3:GetLifecycleConfiguration",
      "s3:GetReplicationConfiguration",
      "s3:ListBucket",
      "s3:ListBucketVersions",
      "s3:PutBucketCORS",
      "s3:PutBucketLogging",
      "s3:PutBucketOwnershipControls",
      "s3:PutBucketPolicy",
      "s3:PutBucketPublicAccessBlock",
      "s3:PutBucketTagging",
      "s3:PutEncryptionConfiguration",
      "s3:PutLifecycleConfiguration",
    ]

    resources = [
      local.staging_media_bucket_arn,
      local.staging_media_logs_bucket_arn,
    ]
  }

  statement {
    sid    = "DeleteStagingMediaObjects"
    effect = "Allow"

    actions = [
      "s3:DeleteObject",
      "s3:DeleteObjectVersion",
    ]

    resources = [
      "${local.staging_media_bucket_arn}/*",
      "${local.staging_media_logs_bucket_arn}/*",
    ]
  }

  statement {
    sid    = "ManageStagingMediaDistribution"
    effect = "Allow"

    actions = [
      "cloudfront:CreateDistribution",
      "cloudfront:CreateDistributionWithTags",
      "cloudfront:DeleteDistribution",
      "cloudfront:GetDistribution",
      "cloudfront:GetDistributionConfig",
      "cloudfront:ListTagsForResource",
      "cloudfront:TagResource",
      "cloudfront:UntagResource",
      "cloudfront:UpdateDistribution",
    ]

    resources = [
      local.staging_media_distribution_arn,
    ]
  }

  statement {
    sid    = "ManageStagingMediaOriginAccessControl"
    effect = "Allow"

    actions = [
      "cloudfront:CreateOriginAccessControl",
      "cloudfront:DeleteOriginAccessControl",
      "cloudfront:GetOriginAccessControl",
      "cloudfront:ListOriginAccessControls",
      "cloudfront:UpdateOriginAccessControl",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "ReadStagingMediaProvisionerPolicy"
    effect = "Allow"

    actions = [
      "iam:GetPolicy",
      "iam:GetPolicyVersion",
      "iam:ListPolicyVersions",
    ]

    resources = [
      local.staging_media_provisioner_policy_arn,
    ]
  }
}

resource "aws_iam_policy" "staging_media_provisioner" {
  name        = "oven-platform-terraform-staging-media"
  description = "Least-privilege permissions for provisioning Oven Platform staging media."

  policy = data.aws_iam_policy_document.staging_media_provisioner.json

  tags = local.required_tags
}

resource "aws_iam_role_policy_attachment" "staging_media_provisioner" {
  role       = aws_iam_role.terraform_provisioner.name
  policy_arn = aws_iam_policy.staging_media_provisioner.arn
}
