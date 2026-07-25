locals {
  state_bucket_arn                 = "arn:${data.aws_partition.current.partition}:s3:::${var.state_bucket_name}"
  monthly_budget_arn               = "arn:${data.aws_partition.current.partition}:budgets::${data.aws_caller_identity.current.account_id}:budget/oven-platform"
  primary_billing_view_arn         = "arn:${data.aws_partition.current.partition}:billing::${data.aws_caller_identity.current.account_id}:billingview/primary"
  terraform_provisioner_policy_arn = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:policy/oven-platform-terraform-guardrails"
}

data "aws_iam_policy_document" "terraform_provisioner" {
  statement {
    sid    = "ListTerraformState"
    effect = "Allow"

    actions = [
      "s3:ListBucket",
    ]

    resources = [
      local.state_bucket_arn,
    ]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values = [
        "staging/terraform.tfstate*",
      ]
    }
  }

  statement {
    sid    = "ReadWriteTerraformState"
    effect = "Allow"

    actions = [
      "s3:GetObject",
      "s3:PutObject",
    ]

    resources = [
      "${local.state_bucket_arn}/staging/terraform.tfstate",
    ]
  }

  statement {
    sid    = "ManageTerraformStateLock"
    effect = "Allow"

    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]

    resources = [
      "${local.state_bucket_arn}/staging/terraform.tfstate.tflock",
    ]
  }

  statement {
    sid    = "ManageOvenPlatformBudget"
    effect = "Allow"

    actions = [
      "budgets:ListTagsForResource",
      "budgets:ModifyBudget",
      "budgets:TagResource",
      "budgets:UntagResource",
      "budgets:ViewBudget",
    ]

    resources = [
      local.monthly_budget_arn,
    ]
  }

  statement {
    sid    = "AccessBillingForBudget"
    effect = "Allow"

    actions = [
      "aws-portal:ModifyBilling",
      "aws-portal:ViewBilling",
      "billing:GetBillingViewData",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "CreateTaggedCostAnomalyResources"
    effect = "Allow"

    actions = [
      "ce:CreateAnomalyMonitor",
      "ce:CreateAnomalySubscription",
    ]

    resources = ["*"]

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
    sid    = "ManageCostAnomalyResources"
    effect = "Allow"

    actions = [
      "ce:DeleteAnomalyMonitor",
      "ce:DeleteAnomalySubscription",
      "ce:GetAnomalyMonitors",
      "ce:GetAnomalySubscriptions",
      "ce:ListTagsForResource",
      "ce:TagResource",
      "ce:UntagResource",
      "ce:UpdateAnomalyMonitor",
      "ce:UpdateAnomalySubscription",
    ]

    resources = [
      aws_ce_anomaly_monitor.services.arn,
      aws_ce_anomaly_subscription.daily.arn,
    ]
  }

  statement {
    sid    = "InspectCurrentSpend"
    effect = "Allow"

    actions = [
      "ce:GetCostAndUsage",
    ]

    resources = [
      local.primary_billing_view_arn,
    ]
  }

  statement {
    sid    = "ListOidcProviders"
    effect = "Allow"

    actions = [
      "iam:ListOpenIDConnectProviders",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "CreateTaggedGitHubOidcProvider"
    effect = "Allow"

    actions = [
      "iam:CreateOpenIDConnectProvider",
    ]

    resources = ["*"]

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
    sid    = "ManageGitHubOidcProvider"
    effect = "Allow"

    actions = [
      "iam:AddClientIDToOpenIDConnectProvider",
      "iam:DeleteOpenIDConnectProvider",
      "iam:GetOpenIDConnectProvider",
      "iam:ListOpenIDConnectProviderTags",
      "iam:RemoveClientIDFromOpenIDConnectProvider",
      "iam:TagOpenIDConnectProvider",
      "iam:UntagOpenIDConnectProvider",
      "iam:UpdateOpenIDConnectProviderThumbprint",
    ]

    resources = [
      aws_iam_openid_connect_provider.github.arn,
    ]
  }

  statement {
    sid    = "ReadProvisionerRole"
    effect = "Allow"

    actions = [
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListRolePolicies",
    ]

    resources = [
      aws_iam_role.terraform_provisioner.arn
    ]
  }

  statement {
    sid    = "ManageGitHubDeploymentRole"
    effect = "Allow"

    actions = [
      "iam:AttachRolePolicy",
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
      "iam:DetachRolePolicy",
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListRolePolicies",
      "iam:PutRolePolicy",
      "iam:TagRole",
      "iam:UntagRole",
      "iam:UpdateAssumeRolePolicy",
      "iam:UpdateRole",
      "iam:UpdateRoleDescription",
    ]

    resources = [
      aws_iam_role.github_staging_deploy.arn,
    ]
  }

  statement {
    sid    = "InspectTerraformStateBucket"
    effect = "Allow"

    actions = [
      "s3:GetAccelerateConfiguration",
      "s3:GetBucket*",
      "s3:GetEncryptionConfiguration",
      "s3:GetLifecycleConfiguration",
      "s3:GetReplicationConfiguration",
      "s3:ListBucket",
    ]

    resources = [
      local.state_bucket_arn,
    ]
  }

  statement {
    sid    = "ReadProvisionerPolicy"
    effect = "Allow"

    actions = [
      "iam:GetPolicy",
      "iam:GetPolicyVersion",
      "iam:ListPolicyVersions",
    ]

    resources = [
      local.terraform_provisioner_policy_arn,
    ]
  }
}

resource "aws_iam_policy" "terraform_provisioner" {
  name        = "oven-platform-terraform-guardrails"
  description = "Least-privilege permissions for Oven Platform Terraform guardrails."

  policy = data.aws_iam_policy_document.terraform_provisioner.json

  tags = local.required_tags
}

resource "aws_iam_role_policy_attachment" "terraform_provisioner_guardrails" {
  role       = aws_iam_role.terraform_provisioner.name
  policy_arn = aws_iam_policy.terraform_provisioner.arn
}
