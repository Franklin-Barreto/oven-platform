variable "aws_region" {
  description = "Single AWS region used by the OVen Platform staging environment"
  type        = string
  default     = "us-east-1"
}

variable "billing_alert_email" {
  description = "Email address that receives AWS budget notifications."
  type        = string
  sensitive   = true
}

variable "state_bucket_name" {
  description = "S3 bucket that stores the Terraform state."
  type        = string
}
