locals {
  staging_ecr_repository_arn     = "arn:${data.aws_partition.current.partition}:ecr:${var.aws_region}:${data.aws_caller_identity.current.account_id}:repository/oven-platform"
  staging_host_role_arn          = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:role/oven-platform-staging-host"
  staging_instance_profile_arn   = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:instance-profile/oven-platform-staging-host"
  staging_provisioner_policy_arn = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:policy/oven-platform-terraform-staging"
}

data "aws_iam_policy_document" "staging_provisioner" {
  statement {
    sid    = "DiscoverStagingInfrastructure"
    effect = "Allow"

    actions = [
      "ec2:Describe*",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "ReadAmazonLinuxAmiParameter"
    effect = "Allow"

    actions = [
      "ssm:GetParameter",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::parameter/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64",
    ]
  }

  statement {
    sid    = "ReadStagingEcrRepository"
    effect = "Allow"

    actions = [
      "ecr:DescribeRepositories",
      "ecr:GetLifecyclePolicy",
      "ecr:ListTagsForResource",
    ]

    resources = [
      local.staging_ecr_repository_arn,
    ]
  }

  statement {
    sid    = "AuthenticateToEcr"
    effect = "Allow"

    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "PushStagingApplicationImage"
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
      local.staging_ecr_repository_arn,
    ]
  }

  statement {
    sid    = "ReadStagingHostIdentity"
    effect = "Allow"

    actions = [
      "iam:GetInstanceProfile",
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:ListRolePolicies",
    ]

    resources = [
      local.staging_host_role_arn,
      local.staging_instance_profile_arn,
    ]
  }

  statement {
    sid    = "CreateTaggedStagingEc2Resources"
    effect = "Allow"

    actions = [
      "ec2:AllocateAddress",
      "ec2:CreateInternetGateway",
      "ec2:CreateRouteTable",
      "ec2:CreateSecurityGroup",
      "ec2:CreateSubnet",
      "ec2:CreateVpc",
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

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "TagStagingEc2ResourcesAtCreation"
    effect = "Allow"

    actions = [
      "ec2:CreateTags",
    ]

    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "ec2:CreateAction"
      values = [
        "AllocateAddress",
        "CreateInternetGateway",
        "CreateRouteTable",
        "CreateSecurityGroup",
        "CreateSubnet",
        "CreateVpc",
      ]
    }
  }

  statement {
    sid    = "ManageTaggedStagingEc2Resources"
    effect = "Allow"

    actions = [
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:DeleteInternetGateway",
      "ec2:DeleteRouteTable",
      "ec2:DeleteSecurityGroup",
      "ec2:DeleteSubnet",
      "ec2:DeleteVolume",
      "ec2:DeleteVpc",
      "ec2:ModifyInstanceAttribute",
      "ec2:ModifySubnetAttribute",
      "ec2:ModifyVpcAttribute",
      "ec2:ReleaseAddress",
      "ec2:RevokeSecurityGroupEgress",
      "ec2:RevokeSecurityGroupIngress",
      "ec2:StartInstances",
      "ec2:StopInstances",
      "ec2:TerminateInstances",
    ]

    resources = ["*"]

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
    sid    = "ConnectStagingNetworkResources"
    effect = "Allow"

    actions = [
      "ec2:AssociateAddress",
      "ec2:AssociateRouteTable",
      "ec2:AttachInternetGateway",
      "ec2:CreateRoute",
      "ec2:DeleteRoute",
      "ec2:DetachInternetGateway",
      "ec2:DisassociateAddress",
      "ec2:DisassociateRouteTable",
    ]

    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "CreateTaggedStagingEcrRepository"
    effect = "Allow"

    actions = [
      "ecr:CreateRepository",
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

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid    = "ManageStagingEcrRepository"
    effect = "Allow"

    actions = [
      "ecr:BatchDeleteImage",
      "ecr:DeleteLifecyclePolicy",
      "ecr:DeleteRepository",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories",
      "ecr:GetLifecyclePolicy",
      "ecr:GetRepositoryPolicy",
      "ecr:ListImages",
      "ecr:ListTagsForResource",
      "ecr:PutImageScanningConfiguration",
      "ecr:PutImageTagMutability",
      "ecr:PutLifecyclePolicy",
      "ecr:TagResource",
      "ecr:UntagResource",
    ]

    resources = [
      local.staging_ecr_repository_arn,
    ]
  }

  statement {
    sid    = "CreateTaggedStagingHostIdentity"
    effect = "Allow"

    actions = [
      "iam:CreateInstanceProfile",
      "iam:CreateRole",
    ]

    resources = [
      local.staging_host_role_arn,
      local.staging_instance_profile_arn,
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
  }

  statement {
    sid    = "ManageStagingHostRole"
    effect = "Allow"

    actions = [
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
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
      local.staging_host_role_arn,
    ]
  }

  statement {
    sid    = "ManageStagingInstanceProfile"
    effect = "Allow"

    actions = [
      "iam:AddRoleToInstanceProfile",
      "iam:DeleteInstanceProfile",
      "iam:GetInstanceProfile",
      "iam:RemoveRoleFromInstanceProfile",
      "iam:TagInstanceProfile",
      "iam:UntagInstanceProfile",
    ]

    resources = [
      local.staging_instance_profile_arn,
    ]
  }

  statement {
    sid    = "AttachSsmCoreToStagingHost"
    effect = "Allow"

    actions = [
      "iam:AttachRolePolicy",
      "iam:DetachRolePolicy",
    ]

    resources = [
      local.staging_host_role_arn,
    ]

    condition {
      test     = "ArnEquals"
      variable = "iam:PolicyARN"
      values = [
        "arn:${data.aws_partition.current.partition}:iam::aws:policy/AmazonSSMManagedInstanceCore",
      ]
    }
  }

  statement {
    sid    = "PassStagingHostRoleToEc2"
    effect = "Allow"

    actions = [
      "iam:PassRole",
    ]

    resources = [
      local.staging_host_role_arn,
    ]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ec2.amazonaws.com"]
    }
  }

  statement {
    sid    = "ReadStagingProvisionerPolicy"
    effect = "Allow"

    actions = [
      "iam:GetPolicy",
      "iam:GetPolicyVersion",
      "iam:ListPolicyVersions",
    ]

    resources = [
      local.staging_provisioner_policy_arn,
    ]
  }

  statement {
    sid    = "CreateNetworkResourcesInTaggedStagingVpc"
    effect = "Allow"

    actions = [
      "ec2:CreateRouteTable",
      "ec2:CreateSecurityGroup",
      "ec2:CreateSubnet",
    ]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:vpc/*",
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
}

resource "aws_iam_policy" "staging_provisioner" {
  name        = "oven-platform-terraform-staging"
  description = "Least-privilege permissions for provisioning Oven Platform staging."

  policy = data.aws_iam_policy_document.staging_provisioner.json

  tags = local.required_tags
}

resource "aws_iam_role_policy_attachment" "staging_provisioner" {
  role       = aws_iam_role.terraform_provisioner.name
  policy_arn = aws_iam_policy.staging_provisioner.arn
}
