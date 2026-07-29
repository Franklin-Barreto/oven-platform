variable "aws_region" {
  description = "AWS region used by the Oven Platform staging environment."
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "IPv4 CIDR assigned to the staging VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr" {
  description = "IPv4 CIDR assigned to the staging public subnet."
  type        = string
  default     = "10.20.1.0/24"
}

variable "instance_type" {
  description = "EC2 instance type used by the staging host."
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "Size in GiB of the encrypted gp3 root volume."
  type        = number
  default     = 20

  validation {
    condition     = var.root_volume_size >= 16
    error_message = "The root volume must have at least 16 GiB."
  }
}

variable "media_cors_allowed_origins" {
  description = "Browser origins allowed to upload media directly to S3."
  type        = list(string)

  default = [
    "https://oven-platform-staging.duckdns.org",
  ]

  validation {
    condition     = length(var.media_cors_allowed_origins) > 0
    error_message = "At least one media CORS origin must be configured."
  }
}
