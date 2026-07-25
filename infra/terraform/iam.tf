resource "aws_iam_openid_connect_provider" "github" {
  client_id_list = ["sts.amazonaws.com"]
  url            = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_role" "github_staging_deploy" {
  name                 = "oven-platform-staging-github-deploy"
  description          = "Temporary credentials for oven-platform GitHub Actions deployments to staging"
  max_session_duration = 3600

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.github.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
            "token.actions.githubusercontent.com:sub" = "repo:Franklin-Barreto/oven-platform:environment:staging"
          }
        }
      }
    ]
  })
}

resource "aws_iam_role" "terraform_provisioner" {
  name                 = "oven-platform-terraform-provisioner"
  description          = ""
  max_session_duration = 3600

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:user/oven-platform-admin"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = local.required_tags
}
