resource "aws_ce_anomaly_monitor" "services" {
  provider = aws.billing

  name              = "Default-Services-Monitor"
  monitor_type      = "DIMENSIONAL"
  monitor_dimension = "SERVICE"

  tags = local.required_tags
}

resource "aws_ce_anomaly_subscription" "daily" {
  provider = aws.billing

  name      = "Default-Services-Subscription"
  frequency = "DAILY"

  monitor_arn_list = [
    aws_ce_anomaly_monitor.services.arn,
  ]

  subscriber {
    type    = "EMAIL"
    address = var.billing_alert_email
  }

  threshold_expression {
    and {
      dimension {
        key           = "ANOMALY_TOTAL_IMPACT_ABSOLUTE"
        match_options = ["GREATER_THAN_OR_EQUAL"]
        values        = ["1"]
      }
    }

    and {
      dimension {
        key           = "ANOMALY_TOTAL_IMPACT_PERCENTAGE"
        match_options = ["GREATER_THAN_OR_EQUAL"]
        values        = ["20"]
      }
    }
  }

  tags = local.required_tags
}
