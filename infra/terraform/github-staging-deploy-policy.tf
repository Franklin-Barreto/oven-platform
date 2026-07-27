locals {
  github_deploy_ecr_repository_arn = "arn:${data.aws_partition.current.partition}:ecr:${var.aws_region}:${data.aws_caller_identity.current.account_id}:repository/oven-platform"
  github_deploy_instance_arn       = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*"
}

data "aws_iam_policy_document" "github_staging_deploy" {
  statement {
    sid    = "AuthenticateToEcr"
    effect = "Allow"

    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "PushApplicationImage"
    effect = "Allow"

    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]

    resources = [
      local.github_deploy_ecr_repository_arn,
    ]
  }

  statement {
    sid    = "InspectStagingHost"
    effect = "Allow"

    actions = [
      "ec2:DescribeInstances",
      "ec2:DescribeInstanceStatus",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "StartTaggedStagingHost"
    effect = "Allow"

    actions = [
      "ec2:StartInstances",
    ]

    resources = [
      local.github_deploy_instance_arn,
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
  }

  statement {
    sid    = "InspectSessionManager"
    effect = "Allow"

    actions = [
      "ssm:DescribeInstanceInformation",
      "ssm:GetCommandInvocation",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "UseRunShellScriptDocument"
    effect = "Allow"

    actions = [
      "ssm:SendCommand",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript",
    ]
  }

  statement {
    sid    = "RunCommandsOnTaggedStagingHost"
    effect = "Allow"

    actions = [
      "ssm:SendCommand",
    ]

    resources = [
      local.github_deploy_instance_arn,
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
}

resource "aws_iam_role_policy" "github_staging_deploy" {
  name   = "oven-platform-staging-deploy"
  role   = aws_iam_role.github_staging_deploy.id
  policy = data.aws_iam_policy_document.github_staging_deploy.json
}