# Clean Architecture --- Architecture Rules

## 1. Dependency Rule

The fundamental rule of Clean Architecture is:

> **Dependencies must point inward, toward the business rules.**

Outer layers may depend on inner layers, but inner layers must not
depend on outer layers.

``` text
Presentation → Application → Domain
                   ↑
              Infrastructure
```

The Domain is the most independent part of the system.

------------------------------------------------------------------------

## 2. Domain Layer

The Domain contains the core business rules and concepts of the system.

It should be independent of:

-   Frameworks
-   Databases
-   UI
-   HTTP
-   External services
-   Infrastructure technologies

The Domain should express **what the business is**, not **how technology
implements it**.

------------------------------------------------------------------------

## 3. Application Layer

The Application layer defines and coordinates application use cases.

It determines:

-   What the system does
-   The sequence of operations
-   Which domain rules are invoked
-   Which external capabilities are required

It should not contain infrastructure-specific implementation details.

------------------------------------------------------------------------

## 4. Infrastructure Layer

Infrastructure contains technical implementations and external
dependencies.

Examples include:

-   Database access
-   ORM
-   External APIs
-   Message brokers
-   File storage
-   Framework-specific implementations

Infrastructure implements abstractions required by inner layers rather
than defining the business rules itself.

------------------------------------------------------------------------

## 5. Presentation Layer

Presentation is responsible for communicating with external actors.

Examples include:

-   REST controllers
-   GraphQL endpoints
-   Web interfaces
-   CLI interfaces

Presentation should translate external input into application requests
and translate application results into external responses.

Business rules should not be placed directly in the presentation layer.

------------------------------------------------------------------------

## 6. Interface / Abstraction Rule

When an inner layer needs functionality provided by an outer layer, the
inner layer should define an abstraction (interface/port).

The outer layer then implements that abstraction.

``` text
Inner Layer
    │
    └── Interface / Port
             ↑
             │
       Infrastructure
```

This prevents business logic from becoming coupled to specific
technologies.

**Note:** The abstraction/port does not have to live in one fixed layer.
Depending on team convention, a port may be defined in the Domain layer
(e.g. a repository interface owned by the business concept it serves) or
in the Application layer (e.g. a port required only to orchestrate a
specific use case). Both placements satisfy the Dependency Rule as long
as the outer layer (Infrastructure) is the one implementing the
abstraction, never the other way around. This rule should not be
interpreted as forcing every port into the Application layer.

------------------------------------------------------------------------

## 7. Framework Independence

Frameworks are implementation details.

The architecture should not make the business rules dependent on a
particular framework.

For example, changing:

-   Spring → another framework
-   PostgreSQL → another database
-   REST → another interface

should not require rewriting the core business rules.

------------------------------------------------------------------------

## 8. Separation of Concerns

Each layer should have a clear responsibility.

``` text
Domain          → Business rules
Application     → Use cases
Infrastructure  → Technical implementation
Presentation    → External communication
```

A component should not take responsibility for concerns belonging to
another layer.

------------------------------------------------------------------------

## 9. Business Modules

When the system contains multiple business areas, it can be organized by
business module first.

``` text
modules/
├── module-a/
├── module-b/
└── module-c/
```

Each module can internally apply the same Clean Architecture principles:

``` text
module-a/
├── domain/
├── application/
├── infrastructure/
└── presentation/
```

This is commonly described as **Modular Clean Architecture**.

------------------------------------------------------------------------

## 10. Cross-Cutting Concerns

Concerns that are shared across the system, such as security, logging,
configuration, monitoring, and error handling, may be separated from
individual business modules.

However, they must still respect the dependency rule.

Being shared does not give a component permission to depend arbitrarily
on business modules.

------------------------------------------------------------------------

## 11. Module Dependency Rule

Business modules should not depend directly on the internal
implementation details of other modules.

Prefer:

``` text
Module A → Public abstraction/contract → Module B
```

instead of:

``` text
Module A → Internal implementation of Module B
```

This keeps modules loosely coupled and allows them to evolve
independently.

------------------------------------------------------------------------

## 12. Core Architectural Principle

Clean Architecture is not defined by a particular folder structure.

The folders are only a way to express architectural boundaries.

The actual architecture is determined by:

1.  **Dependency direction**
2.  **Separation of responsibilities**
3.  **Business-rule independence**
4.  **Abstraction of external dependencies**
5.  **Isolation between business modules**

Therefore:

> **A project is Clean Architecture because its dependencies and
> responsibilities obey these rules, not simply because its folders are
> named `domain`, `application`, `infrastructure`, and `presentation`.**
