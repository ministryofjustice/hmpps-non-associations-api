# 1. Existing NOMIS schema for non-associations

[Next >>](0003-migration-and-sync-with-NOMIS.md)


Date: 2023-06-13

## Status

Accepted

## Context
This illustrates the key fields that NOMIS holds on non-associations

```mermaid
---
title: Non-associations NOMIS ER Diagram
---

classDiagram
direction TB
OFFENDER_NON_ASSOCIATIONS "1" --> "0..1" OFFENDER_NA_DETAILS

class OFFENDER_NON_ASSOCIATIONS {
    long OFFENDER_BOOK_ID PK
    long NS_OFFENDER_BOOK_ID PK
    number[10] OFFENDER_ID
    number[10] NS_OFFENDER_ID
    string[12] NS_REASON_CODE
    string[12] NS_LEVEL_CODE
    string[12] INTERNAL_LOCATION_FLAG
    string[1] TRANSPORT_FLAG
    string[12] RECIP_NS_REASON_CODE
}
    
class OFFENDER_NA_DETAILS {
   long OFFENDER_ID
   long NS_OFFENDER_ID PK
   number TYPE_SEQ PK
   number[10] OFFENDER_BOOK_ID
   number[10] NS_OFFENDER_BOOK_ID
   string[12] NS_REASON_CODE
   string[12] NS_LEVEL_CODE
   string[12] NS_TYPE
   string[60] AUTHORIZED_STAFF
   string[240] COMMENT_TEXT
   string[12] RECIP_NS_REASON_CODE
   date NS_EFFECTIVE_DATE
   date NS_EXPIRY_DATE

}
```

### Gangs Schema

```mermaid
---
title: Gang Non-associations NOMIS ER Diagram
---

classDiagram
direction TB

GANGS "1" --> "0..*" GANG_NON_ASSOCIATIONS
GANGS "1" --> "0..*" OFFENDER_GANG_AFFILIATIONS
OFFENDER_GANG_AFFILIATIONS "1" --> "0..*" OFFENDER_GANG_EVIDENCES
OFFENDER_GANG_AFFILIATIONS "1" --> "0..*" OFFENDER_GANG_INVESTS

class GANGS {
   string[12] GANG_CODE PK
   string[40] GANG_NAME
   number[6] LIST_SEQ
   string[1] ACTIVE_FLAG
   date EXPIRY_DATE
   string[1] UPDATE_ALLOWED_FLAG
   string[12] PARENT_GANG_CODE
}
class GANG_NON_ASSOCIATIONS {
   string[12] GANG_CODE PK
   string[12] NS_GANG_CODE PK
   string[12] NS_REASON_CODE
   string[12] NS_LEVEL_CODE
   string[1] INTERNAL_LOCATION_FLAG
   string[1] TRANSPORT_FLAG
}
class OFFENDER_GANG_AFFILIATIONS {
   string[12] GANG_CODE PK
   number[10] OFFENDER_BOOK_ID PK
   string[240] COMMENT_TEXT
}
class OFFENDER_GANG_EVIDENCES {
   number[10] OFFENDER_BOOK_ID PK
   string[12] GANG_CODE PK
   number[6] OFF_GANG_EVIDENCE_SEQ PK
   string[12] GANG_EVIDENCE_CODE
}
class OFFENDER_GANG_INVESTS {
   number[10] OFFENDER_BOOK_ID PK
   string[12] GANG_CODE PK
   string[12] MEMBERSHIP_STATUS PK
   number[6] INVESTIGATE_SEQ
   string[240] COMMENT_TEXT
}
```

[Next >>](0003-migration-and-sync-with-NOMIS.md)
