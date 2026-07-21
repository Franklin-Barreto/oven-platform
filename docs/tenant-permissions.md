# Tenant Permissions

Tenant permissions are the stable authorization contract for tenant-scoped operations. Membership
profiles describe business personas, while permissions describe the actions the application can
protect. Endpoint authorization must therefore depend on permission names, not profile names.

## Permission Model

The bounded permission vocabulary is defined by `TenantPermission` in the identity application
API. `TenantPermissionResolver` owns the default mapping from membership profiles to effective
permissions:

| Profile | Effective permissions |
| --- | --- |
| `OWNER` | Every tenant permission |
| `MANAGER` | Every tenant permission |
| `ATTENDANT` | `CATALOG_READ`, `CUSTOMER_READ`, `CUSTOMER_MANAGE`, `ORDER_READ`, `ORDER_CREATE`, `ORDER_MANAGE`, `PAYMENT_READ`, `PAYMENT_MANAGE` |
| `KITCHEN` | `KITCHEN_READ`, `KITCHEN_OPERATE` |

A membership can have multiple operational profiles. Its effective permissions are the set union
of every assigned profile's permissions, so overlapping permissions appear only once. The resolver
returns an immutable set.

## Authentication Flow

The active persisted tenant membership is the source of truth on every authenticated request. The
JWT identifies the user and tenant, but does not contain profiles or permissions. After validating
the token, authentication loads the current membership, derives its effective permissions, and
adds one Spring Security authority for each permission.

This means profile changes and membership deactivation take effect on the next request without
issuing a new token. Permissions must not be accepted from request headers, bodies, query
parameters, or other client-controlled data.

## Extension Convention

When introducing a protected tenant capability:

1. Add its permission name to `TenantPermission`. Prefer action-oriented names grouped by business
   capability, following the existing `<CAPABILITY>_<ACTION>` convention.
2. Update the centralized mappings in `TenantPermissionResolver`. Do not duplicate mappings in
   controllers, security configuration, or persistence.
3. Update the exhaustive resolver tests for every affected profile and any relevant combined
   profile scenario.
4. Use the permission name as the Spring Security authorization contract. Do not authorize an
   endpoint using `OWNER`, `MANAGER`, `ATTENDANT`, or `KITCHEN` directly.

When changing an existing profile, modify only its centralized mapping and corresponding tests.
The predefined mapping is code-owned and version controlled: it is not persisted per membership,
stored in JWT claims, or assigned directly to individual users.
