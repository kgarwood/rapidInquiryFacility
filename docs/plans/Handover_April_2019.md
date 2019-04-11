---
layout: default
title: Handover Notes for the Suspension of Development, April 2019
---

# Martin

## Status of Recent Work

Everything is up to date. The installer has been tested on Windows and Mac with both databases and works reliably. Multiple covariates work.


## Things I wanted to get done

There are [51 open issues](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues) at the time of writing. Some of them are mentioned below, but all are things that we could do with fixing, building, or improving.

### Making the Installer Handle Updates

The installer creates a brand-new database. It would be good to make it also handle upgrades from one version to the next. See [issue #146](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/146).

### Local Caching of Base Maps

See [issue #89](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/89). Support for using maps when no internet connection is available.

### Better Database Schema Management

See [issue #147](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/147). At present we maintain two sets of scripts for creating and updating the database schemas -- one for each platform. We'd like to move to using a single set of scripts to the extent that is possible, and Liquibase is a useful tool to help with that.

### Improving Database Access

I'd like to restructure the code so that the database access was all in classes that are intended for that. At present things are a bit of a mess, with SQL spread through various parts of the Java code.

Ideally, I think, I would move the whole database access to a separate microservice. This isn't strictly necessary, and it may be splitting things up further than is strictly needed, but it would be a good way to enforce the separation of concerns.

### The Roadmap

Not forgetting everything on [the RIF Roadmap](https://trello.com/b/CTTtyxJR/the-rif-roadmap).

## Contacting Me

If you need me to look at anything you can email me at <martin@devilgate.org>. I'm on Twitter at [@devilgate](https://twitter.com/devilgate). Also [LinkedIn](https://www.linkedin.com/in/martinmccallion/?originalSubdomain=uk), [Facebook](http://www.facebook.com/martin.mccallion).