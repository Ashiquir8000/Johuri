# Johuri.ai — System Architecture & Developer Context

> **Tagline:** AI-Powered Creator Valuation & Enterprise ERP Platform for Bangladeshi Content Creators.  
> **Core Metaphor & Brand Identity:** *"জহুরিই চেনে জহরত"* (Only a jeweler recognizes a gem). Johuri.ai acts as an intelligent AI-powered enterprise platform that evaluates talented content creators (Gems) as digital assets, automates financial valuation, bridges brand sponsorships, and manages creator business operations.

---

## 1. System Overview & Core Capabilities

**Johuri.ai** is an All-in-One Creator Enterprise Resource Planning (ERP) & Operations SaaS Platform designed for the Bangladeshi creator economy. It eliminates manual business workflows (chatboxes, spreadsheets) by integrating:

- **Live Analytics Telemetry:** Dynamic YouTube Data API v3 integration for authenticated stats.
- **Hybrid Claim & Verification Architecture:** Security-first OAuth 2.0 flow to prevent identity impersonation.
- **AI Sponsorship Negotiation Engine:** Google Gemini API integration for brand email evaluation and rate recommendation.
- **Kanban Deal Pipeline & Automated Billing:** Drag-and-drop CRM with automated bKash Sandbox/SSLCommerz invoicing.
- **Digital Storefront:** Automated asset delivery (e-books, LUTs, presets) upon payment verification.

---

## 2. Tech Stack & Infrastructure

- **Language & Runtime:** Java 17
- **Backend Framework:** Spring Boot 3.x (Spring Security, WebClient, Spring Data MongoDB)
- **Database:** MongoDB (NoSQL)
- **Base Package:** `com.example.johuri`
- **External Integrations:**
  - **YouTube Data API v3:** Public channel performance telemetry.
  - **Google Gemini API:** AI-driven rate valuation & counter-proposal draft generation.
  - **bKash Sandbox / SSLCommerz:** Payment gateway & webhook listener.
  - **Google OAuth 2.0:** Secure channel verification & account claiming.

---

## 3. Directory & Package Structure Reference

```text
com.example.johuri
├── config/         # Spring Security, CORS, OAuth2, Gemini WebClient Beans
├── controller/     # REST Endpoints (User, Creator, Deal, AI, Payment, Store)
├── DTO/            # Request & Response Data Transfer Objects (Validation Enabled)
├── entity/         # MongoDB Documents (@Document)
├── repository/     # Spring Data MongoDB Repositories (@Repository)
├── service/        # Core Business Logic & External Integrations
└── JohuriApplication.java
```

---

## 4. User Roles & Access Control (RBAC)

1. **Creator (`ROLE_CREATOR` - Primary User / Admin):**
   - Full control over dashboard analytics, rate cards, financial reports, payment channels, and team settings.
2. **Brand / Sponsor (`ROLE_BRAND` / Guest):**
   - Access to public media kits (`johuri.ai/rifat`), live channel analytics, digital service packages, and direct inquiry submission.
3. **Team Member (`ROLE_TEAM` - Editor / Designer):**
   - Restricted access to assigned video tasks and draft upload statuses.
   - **Security Boundary:** Cannot view creator financial reports, earnings, or bKash merchant details.

---

## 5. MongoDB Data Models (`entity/`)

### A. `User.java` (Collection: `users`)
- `id` (String, `@Id`)
- `email` (String, Unique)
- `name` (String)
- `roles` (List<String>: `ROLE_CREATOR`, `ROLE_BRAND`, `ROLE_TEAM`)
- `connectedYouTubeChannelId` (String, Optional)
- `createdAt` (Instant)

### B. `CreatorProfile.java` (Collection: `creator_profiles`)
- `id` (String, `@Id`)
- `userId` (String, FK -> User)
- `youtubeHandle` (String) — e.g., `@NadirOnTheGo`
- `verificationStatus` (Enum: `UNCLAIMED`, `VERIFIED`)
- `publicStats` (Nested Object: subscriberCount, totalViews, totalVideos, avgViews)
- `servicePackages` (List<ServicePackage>: title, description, priceBDT)
- `paymentDetails` (Nested Object: bkashMerchantNumber, bankAccount)

### C. `Deal.java` (Collection: `deals`)
- `id` (String, `@Id`)
- `creatorId` (String)
- `brandName` (String)
- `brandEmail` (String)
- `agreedAmountBDT` (Double)
- `status` (Enum: `IN_DISCUSSION`, `SCRIPTING`, `EXECUTION`, `INVOICED`, `PAID`)
- `invoicePdfUrl` (String)
- `createdAt` (Instant)

### D. `DigitalProduct.java` (Collection: `digital_products`)
- `id` (String, `@Id`)
- `creatorId` (String)
- `title` (String)
- `fileDownloadUrl` (String)
- `priceBDT` (Double)

---

## 6. Core System Workflows & Business Logic

### A. Public Data Fetching vs. Profile Claiming
1. **Unclaimed Public Search:** Searching a handle (e.g., `@RafsanTheChotoBhai`) triggers `YouTubeDataService` to query YouTube API v3. The generated profile displays live public stats but remains in **`UNCLAIMED`** status with locked payment settings.
2. **Claiming Flow:** The creator clicks **"Claim Profile"** and authenticates via **Google OAuth 2.0**. The backend matches the channel owner email, updates status to **`VERIFIED`**, and enables custom bKash/rate-card configuration.

### B. AI Rate Negotiation Engine (`GeminiAIService.java`)
1. **Input:** Creator pastes brand sponsorship email body into the dashboard.
2. **AI Analysis:** Backend calls Google Gemini API along with creator metrics (`subscriberCount`, `avgViews`).
3. **Output:** Gemini evaluates fair market valuation (in BDT) and drafts an automated, professional counter-proposal email.

### C. Kanban Deal Pipeline & Automated Billing
1. Sponsorships progress through states: `IN_DISCUSSION` $\rightarrow$ `SCRIPTING` $\rightarrow$ `EXECUTION` $\rightarrow$ `INVOICED` $\rightarrow$ `PAID`.
2. Moving to `INVOICED` generates a bKash Sandbox payment link and automated PDF invoice.
3. Successful bKash payment triggers a webhook listener, transitioning the deal status to `PAID`.

### D. Environment Profiling Strategy (`resources/application.yml`)
- **`dev` Profile:** Enables `CommandLineRunner` data seeding for offline academic demos (pre-seeded rate cards, test deals, and sandbox bKash numbers).
- **`prod` Profile:** Disables data seeder; enforces strict OAuth 2.0 verification and live API integration.

---

## 7. Practical Academic Presentation Flow (Evaluation Day)

When presenting to faculty, follow this 3-step demonstration sequence:

1. **Step 1 (Public Telemetry):** Search `@RafsanTheChotoBhai`. Show live YouTube public stats pulled via API, emphasizing the **`Unverified (Unclaimed)`** security badge.
2. **Step 2 (Live OAuth Verification):** Have the evaluator click **"Connect YouTube"**. Authenticate via Google OAuth 2.0 to show real-time channel telemetry retrieval and owner claim logic.
3. **Step 3 (AI & Business Sandbox):** Log into `rifat_creator` (Seeded Dev Profile). Paste a brand offer email into the Gemini AI box, generate a rate evaluation & counter-proposal, drag a deal to `INVOICED` on the Kanban board, complete a bKash Sandbox payment, and auto-download the generated PDF invoice.

---

## 8. Development Guidelines for AI Coding Assistants

1. **DTO Pattern:** Do NOT expose raw MongoDB entities (`@Document`) directly in `@RestController` endpoints. Always map to and from DTOs.
2. **Validation:** Use Jakarta validation (`@NotNull`, `@NotBlank`, `@Email`) on DTO classes and annotate parameters with `@Valid` in controllers.
3. **Response Standard:** Return `ResponseEntity<ApiResponse<T>>` across all endpoints for uniform API responses.
4. **Exception Handling:** Handle errors via a `@ControllerAdvice` global exception handler.
5. **Lombok Usage:** Apply `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@Builder` across models and DTOs.