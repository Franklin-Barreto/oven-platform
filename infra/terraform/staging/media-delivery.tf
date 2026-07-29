locals {
  media_origin_id                 = "staging-media-s3"
  cloudfront_caching_optimized_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"
}

resource "aws_cloudfront_origin_access_control" "media" {
  name                              = "oven-platform-staging-media"
  description                       = "Private S3 access for staging media delivery."
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# CloudFront access logs are intentionally omitted in staging. The private S3 origin already has
# server access logging, while cache-level analytics do not justify an ACL-enabled legacy log
# bucket or the additional delivery infrastructure for this environment.
resource "aws_cloudfront_distribution" "media" {
  enabled         = true
  is_ipv6_enabled = true
  comment         = "Public delivery for Oven Platform staging media."
  http_version    = "http2and3"
  price_class     = "PriceClass_100"

  origin {
    domain_name              = aws_s3_bucket.media.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.media.id
    origin_id                = local.media_origin_id
  }

  default_cache_behavior {
    allowed_methods = [
      "GET",
      "HEAD",
      "OPTIONS",
    ]

    cached_methods = [
      "GET",
      "HEAD",
      "OPTIONS",
    ]

    cache_policy_id        = local.cloudfront_caching_optimized_id
    compress               = true
    target_origin_id       = local.media_origin_id
    viewer_protocol_policy = "redirect-to-https"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    # The CloudFront default certificate fixes this setting to TLSv1. A stricter minimum requires
    # a custom domain and ACM certificate, which staging does not provision for media delivery.
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "oven-platform-staging-media"
  }
}