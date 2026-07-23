# Work Shift Management & Field Operations Business Process Specification

This document details the business processes for **Work Shift Management**, the **Staff Dashboard**, and the **Daily Schedule** of the Sport Field Booking system. All diagrams are structured horizontally (`flowchart LR`) for optimal rendering in Mermaid tools.

---

## 1. Process 1: Work Shift Configuration (Owner & System)

This workflow represents the activities performed by the **Owner** to configure shifts, assign staff, and manage historical schedules, alongside the validation and persistence rules enforced by the **System**.

```mermaid
flowchart LR
    subgraph LaneOwner ["OWNER"]
        O_Start([Enter Work Shift Management]) --> O_ViewDash[View overview metrics & staff cumulative shift counts]
        O_ViewDash --> O_ChooseAction{Select Action?}
        
        %% Add Shift Branch
        O_ChooseAction -->|Create shift| O_AddForm[Enter shift info: Date, Time range, Complex, Select staff]
        O_AddForm --> O_SelectMode{Choose Mode?}
        O_SelectMode -->|Single| O_SubmitSingle[Submit single shift creation]
        O_SelectMode -->|Batch| O_SubmitBatch[Submit recurring batch shift creation by Days of Week]

        %% Edit / Delete Shift Branch
        O_ChooseAction -->|Edit or Delete shift| O_CheckPast{Is shift in the past?}
        O_CheckPast -->|Yes| O_Block[Display Read-Only state - Lock modifications/deletions]
        O_CheckPast -->|No| O_ModForm[Perform Edit shift / Assign / Unassign / Delete single / Delete batch]
    end

    subgraph LaneSystem ["SYSTEM"]
        S_Validate{Check for overlaps: 1. Facility overlap - 2. Employee overlap?}
        S_SaveDB[(Save shift to DB & Auto-name by time range)]
        S_DeleteDB[(Delete shift & corresponding assignments from DB)]
    end

    %% Inter-lane interactions
    O_SubmitSingle --> S_Validate
    O_SubmitBatch --> S_Validate
    O_ModForm --> S_Validate
    
    S_Validate -->|Overlap or Error| O_AddForm
    S_Validate -->|Valid| S_SaveDB
    S_SaveDB --> O_ViewDash
    
    O_ModForm -.->|Request delete shift| S_DeleteDB
    S_DeleteDB --> O_ViewDash
```

### Shift Configuration Business Rules:
1. **Cumulative Shift Counts**: Helps the Owner optimize human resource allocation by tracking the total number of shifts completed by each employee.
2. **Auto-naming Convention**:
   * Hours `08:00 - 12:00`: Auto-named `Morning Shift + [Complex Short Name]`.
   * Hours `12:00 - 18:00`: Auto-named `Afternoon Shift + [Complex Short Name]`.
   * Hours `18:00 - 22:00`: Auto-named `Evening Shift + [Complex Short Name]`.
   * Other hours: Auto-named `Split Shift + [Complex Short Name]`.
3. **Overlap Constraints**:
   * **Facility Overlap**: A complex cannot have more than 1 work shift scheduled for the same time slot on the same day.
   * **Employee Overlap**: A staff member cannot be assigned to multiple shifts with overlapping hours on the same day.
4. **Historical Lock**: Any shift that has ended (in the past) is automatically locked in a "Read-Only" state to maintain database integrity.

---

## 2. Process 2: Field Operations & Scheduling (Staff, System & Customer)

This workflow represents the operations performed by the **Staff** during their shift, the journey of the **Customer** checking in/playing/checking out, and the updates executed by the **System**.

```mermaid
flowchart LR
    subgraph LaneStaff ["STAFF"]
        St_Login([Enter Staff Dashboard]) --> St_CheckShift{Determine shift state?}
        
        %% Outside shift hours
        St_CheckShift -->|Upcoming or Completed| St_Lock[Display Shift Error: Lock all Check-in / Checkout actions]
        
        %% Inside shift hours
        St_CheckShift -->|Ongoing| St_Dashboard[Activate Dashboard: Show real-time clock, shift progress & KPI statistics]
        
        St_Dashboard --> St_ChooseJob{Select Task?}

        %% Branch 1: Daily Schedule
        St_ChooseJob -->|View Daily Schedule| St_Timeline[View Timeline Grid from 05:00 to 22:00]
        St_Timeline --> St_FilterDate{Filter another date?}
        St_FilterDate -->|Yes| St_QueryDate[Filter date on timeline grid]
        St_FilterDate -->|No| St_FieldStatus{Update field status?}
        St_FieldStatus -->|Yes: Active/Maintenance| St_ChangeField[Open Modal & Update field operational status]
        St_FieldStatus -->|No| St_Timeline

        %% Branch 2: Check-in booking
        St_ChooseJob -->|Search Booking| St_Search[Search booking by booking ID / customer phone / name]
        St_Search --> St_VerifyLate{Customer late > 30 mins?}
        St_VerifyLate -->|Yes| St_NoShow[Cancel booking due to late customer - No-show]
        St_VerifyLate -->|No / On Time| St_ConfirmCheckin[Fill check-in notes & Confirm customer check-in]

        %% Branch 3: Checkout
        St_ChooseJob -->|Checkout| St_Checkout[Initiate Checkout & Generate Invoice with additional services]
        St_Checkout --> St_CollectPayment[Collect cash at counter or Request customer to scan VietQR/VNPay]
    end

    subgraph LaneSystem ["SYSTEM"]
        S_Timer[Shift status scheduler: Upcoming / Ongoing / Completed]
        S_UpdateField[(Update field status & booking status in DB)]
        S_AddKPI[Record successful payment & Add amount to cash KPI]
    end

    subgraph LaneCustomer ["CUSTOMER"]
        C_Arrive([Arrive at stadium & present booking details])
        C_Play[Enter field & play match with extra services]
        C_Payment[Make payment & Receive invoice]
    end

    %% Inter-lane connections
    St_Login --> S_Timer --> St_CheckShift
    
    %% Check-in flow
    C_Arrive --> St_Search
    St_NoShow --> S_UpdateField
    St_ConfirmCheckin --> S_UpdateField
    S_UpdateField -->|Update field status to Playing| C_Play
    
    %% Field maintenance flow
    St_ChangeField --> S_UpdateField

    %% Checkout flow
    C_Play --> St_Checkout
    St_CollectPayment --> C_Payment
    C_Payment --> S_UpdateField
    S_UpdateField -->|Update field status to Available| S_AddKPI
    S_AddKPI -->|Update cash KPI on Dashboard| St_Dashboard
```

### Shift Operations Business Rules:
1. **Time Constraints**: To ensure billing and auditing accuracy, staff are only permitted to perform Check-in/Checkout operations while their assigned shift is in the **Ongoing** state.
2. **Timeline Grid**: Renders booking allocations from `05:00` to `22:00`. Allows date switching and manual updates to field statuses (e.g., setting a field to *Maintenance* blocks online bookings for that slot).
3. **No-Show Cancellation**: If a customer is late by more than 30 minutes, staff can cancel the booking under a "No-show" status, freeing up the field slot for walk-in customers.
4. **Cash KPI Synchronization**: All completed invoice collections (Cash or Digital QR/Gateway) instantly increment the staff's Cash KPI total on their dashboard, ensuring accurate cash drawer reconciliations at the end of the shift.

---

## 3. Process 3: Detailed Customer Check-in Flow (Staff, Customer & System)

This workflow maps out the granular, step-by-step check-in procedure, search handling, facility checks, check-in note entry, and field status transitions.

```mermaid
flowchart LR
    subgraph LaneCustomer ["CUSTOMER"]
        C_Present([Arrive & present Booking ID, Phone, or Name])
        C_GetField([Get physical field hand-over & play])
    end

    subgraph LaneStaff ["STAFF"]
        St_Input[Enter query into search box & submit form]
        St_Prompt[Request Customer to double check details]
        St_Select[Select matching booking from search results list]
        St_CheckLateCheck{Is customer late > 30 minutes?}
        St_TriggerCancel[Click 'Cancel due to late customer' No-Show button]
        St_Modal[Open Check-in Modal & fill note: borrowed training bibs, ball, water]
        St_ClickCheckin[Confirm check-in]
    end

    subgraph LaneSystem ["SYSTEM"]
        S_SearchAPI{Search DB for bookings in Awaiting Check-in status?}
        S_SecurityCheck{Does booking complex ID match staff's current shift complex ID?}
        S_RedirectError[Redirect to schedule with error: facility_mismatch]
        S_DBCancel[(Update booking status to No-Show & free up field)]
        S_DBCheckin[(Update booking to Checked-in, Field status to Playing, log check-in details)]
    end

    %% Flow transitions
    C_Present --> St_Input
    St_Input --> S_SearchAPI
    
    S_SearchAPI -->|No match found| St_Prompt
    St_Prompt --> St_Input
    
    S_SearchAPI -->|Match found| St_Select
    St_Select --> S_SecurityCheck
    
    S_SecurityCheck -->|Complex mismatch| S_RedirectError
    S_SecurityCheck -->|Complex matches| St_CheckLateCheck
    
    St_CheckLateCheck -->|Yes| St_TriggerCancel
    St_TriggerCancel --> S_DBCancel
    
    St_CheckLateCheck -->|No / Within time| St_Modal
    St_Modal --> St_ClickCheckin
    St_ClickCheckin --> S_DBCheckin
    
    S_DBCheckin --> C_GetField
```

### Detailed Check-in Business Rules:
1. **Search Parameters**: Staff can lookup bookings in real-time by entering the Booking ID (e.g., `BK001`), the Customer's phone number, or the Customer's name.
2. **Facility Authorization**: To prevent staff from unauthorized cross-facility alterations, the System strictly blocks check-in requests if the booking belongs to a different complex than the staff's currently active shift.
3. **Check-in Notes**: Rented equipment (bibs, soccer balls, etc.) are typed into the check-in notes textarea to verify inventory during return checkout.
4. **Visual Synchronization**: Upon successful check-in, the System transitions the booking state to `Checked-in` and the field state to `Playing`, turning the grid cell green instantly on the daily schedule layout.
