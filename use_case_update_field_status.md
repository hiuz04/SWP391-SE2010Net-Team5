# Use Case Specification: Update Field Status

| Field | Description |
| :--- | :--- |
| **Use Case Name** | Update Field Status |
| **Primary Actor** | Staff |
| **Secondary Actors** | None |
| **Description** | As a Staff member, I want to manually change the operational state of a specific field (e.g., available, under cleaning, or out of order) to maintain accurate field utilization and booking availability. |
| **Preconditions** | 1. Staff is logged in.<br>2. Staff has an active shift at the booking football complex. |
| **Postconditions** | 1. Field status is updated in the database.<br>2. The updated status is immediately displayed on the daily schedule grid.<br>3. Future bookings are restricted if the field is set to MAINTENANCE or DISABLED. |
| **Normal Sequence/Flow**| 1. Staff selects the schedule view for their complex.<br>2. System displays the interactive timeline grid of all fields.<br>3. Staff clicks on the status badge of a specific field.<br>4. System verifies the staff's active shift and displays the "Update Field Status" modal showing the field's current status (AVAILABLE, MAINTENANCE, or DISABLED).<br>5. Staff selects the new status and clicks "Save changes".<br>6. System updates the field status in the database.<br>7. System reloads the schedule grid with the updated field status and displays a success notification. |
| **Alternative Sequences/Flows** | **A1 - Staff has no active shift at the complex**:<br>System rejects the action and displays an error message: *"You do not have an active shift at this facility."*<br><br>**A2 - The shift is upcoming or ended**:<br>System rejects the status change and informs the staff that the action is not allowed outside shift hours.<br><br>**A3 - The field is currently occupied by an active match (CHECKED_IN booking)**:<br>System allows the change but warns the staff, or updates the status while ensuring the active match is not disrupted. |
