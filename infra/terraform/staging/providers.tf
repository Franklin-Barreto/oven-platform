locals {
  required_tags = {
    project     = "oven-platform"
    environment = "staging"
    managed-by  = "terraform"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.required_tags
  }
}
