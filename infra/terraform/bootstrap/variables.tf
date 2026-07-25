variable "aws_region" {
  description = "AWS region that stores the Terraform state."
  type        = string
  default     = "us-east-1"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name used by the Terraform backend."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$", var.state_bucket_name))
    error_message = "The bucket name must contain 3-63 lowercase letters, numbers, or hyphens."
  }
}
