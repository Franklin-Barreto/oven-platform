resource "aws_budgets_budget" "monthly_cost" {
  provider = aws.billing

  name             = "oven-platform"
  budget_type      = "COST"
  limit_amount     = "5"
  limit_unit       = "USD"
  time_unit        = "MONTHLY"
  tags             = local.required_tags
  billing_view_arn = "arn:${data.aws_partition.current.partition}:billing::${data.aws_caller_identity.current.account_id}:billingview/primary"

  metrics = [
    "UnblendedCost",
  ]

  filter_expression {
    not {
      dimensions {
        key = "RECORD_TYPE"
        values = [
          "Credit",
          "Refund",
        ]
      }
    }
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 50
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.billing_alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 85
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.billing_alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.billing_alert_email]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.billing_alert_email]
  }
}
