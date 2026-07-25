output "instance_id" {
  description = "EC2 instance ID used by operational start, stop, and SSM commands."
  value       = aws_instance.host.id
}

output "public_ip" {
  description = "Stable public IPv4 address assigned to the staging host."
  value       = aws_eip.host.public_ip
}

output "availability_zone" {
  description = "Availability Zone selected for the staging host."
  value       = aws_subnet.public.availability_zone
}

output "ecr_repository_url" {
  description = "ECR repository URL used to push and pull application images."
  value       = aws_ecr_repository.application.repository_url
}

output "ssm_start_session_command" {
  description = "Command used to open a Session Manager shell on the host."
  value       = "aws ssm start-session --target ${aws_instance.host.id} --profile oven-terraform-scoped"
}
