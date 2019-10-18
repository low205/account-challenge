## Account challenge

We need to build an RESTful application which should allow a user to transfer arbitrary amount from one account to another   
Main goals is to keep code simple and concise. Full pledge production ready system is no-goal. 
In the same time we should avoid using widely used enterprise frameworks (especially Spring).   

### Domain
As we have no intent to use this application in production environment we should not delve too deep in Accounting systems. 
We have no goal to implement complete Double Entry Bookkeeping system, Balancing and End-Of-Day, End-Of-Month reporting etc.

System should:

    * Store account data
    * Retrieve account data
    * Validate and Store transactions between accounts
    * Retrieve current balance of account based on transactions stored

Storing and supporting separate Balance entities and subsystems is no-goal.  

### Framework choice
As a framework of choice we decided to use Kotlin's Ktor framework.
Main reasons to be:

    * Kotlin based
    * Lean and configurable    
    * Testable
    * Coroutines are first class citizens

### Concurrency considerations

Using Coroutines, Flows and Channels should allow us to use structured concurrency and avoid using unnecessary locks and synchronisation.

### Deployemnt

Application should be packed as a docker image. 
