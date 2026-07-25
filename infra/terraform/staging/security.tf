resource "aws_security_group" "host" {
  name        = "oven-platform-staging-host"
  description = "Public web traffic for the Oven Platform staging host."
  vpc_id      = aws_vpc.staging.id

  tags = {
    Name = "oven-platform-staging-host"
  }
}

resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.host.id
  description       = "Public HTTP traffic."
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.host.id
  description       = "Public HTTPS traffic."
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.host.id
  description       = "Allow the host to reach package repositories and AWS APIs."
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
