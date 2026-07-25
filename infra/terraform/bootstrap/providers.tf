locals {
  required_tags = {
    project     = "oven-platform"
    environment = "staging"
    managed-by  = "terraform"
    purpose     = "terraform-state"
  }
}

provider "aws" {
  region = var.aws_region
}
