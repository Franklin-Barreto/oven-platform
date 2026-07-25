locals {
  staging_compute_provisioner_policy_arn = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:policy/oven-platform-terraform-staging-compute"
}

data "aws_iam_policy_document" "staging_compute_provisioner" {
  statement {
    sid    = "UseAmazonLinuxImage"
    effect = "Allow"

    actions = [
      "ec2:RunInstances",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}::image/ami-*",
    ]

    condition {
      test     = "StringEquals"
      variable = "ec2:Owner"
      values   = ["amazon"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "UseTaggedStagingNetwork"
    effect = "Allow"

    actions = [
      "ec2:RunInstances",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:security-group/*",
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:subnet/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "ec2:ResourceTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "ec2:ResourceTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "ec2:ResourceTag/managed-by"
      values   = ["terraform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "CreateStagingPrimaryNetworkInterface"
    effect = "Allow"

    actions = [
      "ec2:RunInstances",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:network-interface/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "LaunchTaggedStagingInstance"
    effect = "Allow"

    actions = [
      "ec2:RunInstances",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "ec2:InstanceType"
      values   = ["t3.small"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/managed-by"
      values   = ["terraform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "CreateTaggedStagingRootVolume"
    effect = "Allow"

    actions = [
      "ec2:RunInstances",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:volume/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/managed-by"
      values   = ["terraform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "TagStagingComputeAtLaunch"
    effect = "Allow"

    actions = [
      "ec2:CreateTags",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*",
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:volume/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "ec2:CreateAction"
      values   = ["RunInstances"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/managed-by"
      values   = ["terraform"]
    }
  }

  statement {
    sid    = "CreateTaggedStagingSecurityGroupRules"
    effect = "Allow"

    actions = [
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:AuthorizeSecurityGroupIngress",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:security-group-rule/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/managed-by"
      values   = ["terraform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "TagStagingSecurityGroupRulesAtCreation"
    effect = "Allow"

    actions = [
      "ec2:CreateTags",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:security-group-rule/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "ec2:CreateAction"
      values = [
        "AuthorizeSecurityGroupEgress",
        "AuthorizeSecurityGroupIngress",
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/managed-by"
      values   = ["terraform"]
    }
  }

  statement {
    sid    = "InspectStagingSessionManager"
    effect = "Allow"

    actions = [
      "ssm:DescribeInstanceInformation",
      "ssm:DescribeSessions",
      "ssm:GetConnectionStatus",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "StartSessionOnTaggedStagingHost"
    effect = "Allow"

    actions = [
      "ssm:StartSession",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/project"
      values   = ["oven-platform"]
    }

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/environment"
      values   = ["staging"]
    }

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/managed-by"
      values   = ["terraform"]
    }
  }

  statement {
    sid    = "UseDefaultStagingSessionDocument"
    effect = "Allow"

    actions = [
      "ssm:StartSession",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:document/SSM-SessionManagerRunShell",
    ]
  }

  statement {
    sid    = "UseOwnStagingSessionChannel"
    effect = "Allow"

    actions = [
      "ssmmessages:OpenDataChannel",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ssm:*:*:session/$${aws:userid}-*",
    ]
  }

  statement {
    sid    = "ManageOwnStagingSessions"
    effect = "Allow"

    actions = [
      "ssm:ResumeSession",
      "ssm:TerminateSession",
    ]

    resources = ["*"]

    condition {
      test     = "StringLike"
      variable = "ssm:resourceTag/aws:ssmmessages:session-id"
      values   = ["$${aws:userid}*"]
    }
  }

  statement {
    sid    = "ReadStagingComputeProvisionerPolicy"
    effect = "Allow"

    actions = [
      "iam:GetPolicy",
      "iam:GetPolicyVersion",
      "iam:ListPolicyVersions",
    ]

    resources = [
      local.staging_compute_provisioner_policy_arn,
    ]
  }
}

resource "aws_iam_policy" "staging_compute_provisioner" {
  name        = "oven-platform-terraform-staging-compute"
  description = "Restricted EC2 launch permissions for Oven Platform staging."

  policy = data.aws_iam_policy_document.staging_compute_provisioner.json

  tags = local.required_tags
}

resource "aws_iam_role_policy_attachment" "staging_compute_provisioner" {
  role       = aws_iam_role.terraform_provisioner.name
  policy_arn = aws_iam_policy.staging_compute_provisioner.arn
}
