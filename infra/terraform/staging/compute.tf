resource "aws_instance" "host" {
  ami                         = data.aws_ssm_parameter.amazon_linux_2023_ami.value
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.host.id]
  iam_instance_profile        = aws_iam_instance_profile.host.name
  associate_public_ip_address = false

  monitoring                  = false
  user_data                   = file("${path.module}/user-data.sh")
  user_data_replace_on_change = false

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
    # Docker adds one network hop between the application container and IMDS.
    http_put_response_hop_limit = 2
    instance_metadata_tags      = "enabled"
  }

  credit_specification {
    cpu_credits = "standard"
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    encrypted             = true
    delete_on_termination = true
  }

  volume_tags = merge(local.required_tags, {
    Name = "oven-platform-staging-root"
  })

  tags = {
    Name = "oven-platform-staging"
  }

  lifecycle {
    # The public SSM parameter always resolves to the latest Amazon Linux release. Replacing this
    # singleton host automatically would discard its root volume and interrupt staging. Host
    # upgrades are therefore reviewed and triggered explicitly with terraform apply -replace.
    # The subnet must not assign an automatic public IP. The separately managed Elastic IP makes
    # the provider report this computed attribute as true after association.
    ignore_changes = [ami, associate_public_ip_address]
  }

  depends_on = [
    aws_route.public_internet,
    aws_route_table_association.public,
  ]
}

resource "aws_eip" "host" {
  domain   = "vpc"
  instance = aws_instance.host.id

  tags = {
    Name = "oven-platform-staging"
  }
}
