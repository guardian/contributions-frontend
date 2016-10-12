# Contributions Frontend


## Table of Contents

1. [Getting Started](#getting-started)
1. [Setup](#setup)
1. [Run](#run)
1. [Testing](#testing)
1. [Deployment](#deployment)
1. [Security](#security)

## Getting Started

To get started working on Contributions you will need to complete the following steps:

1. Work through the [Setup](#setup) instructions for this project
2. **Optional:** Work through the setup instructions for the [identity frontend](https://github.com/guardian/identity-frontend). This provides a frontend for sign-up and registration.
3. Start up Contributions by running the commands in the [Run](#run) section of this README

## Setup

### Requirements

We require the following to be installed:

- `Java 8`
- `Node (with npm)`
- `NGINX`

### Install client-side dependencies

```
npm install
npm run devSetup
```

A way to check everything is setup correctly is to run the tests

```
npm test
```

For development you'll may need the following commands:

**Compile assets**

_This is optional as it is handled by `sbt devrun`_

```
npm run compile
```

**Watch files for changes**

_This is optional as it is handled by `sbt devrun`_

```
npm run watch
```

This runs [webpack-dev-server](https://webpack.github.io/docs/webpack-dev-server.html) on port 9111, proxying requests to 9112. In this case, the bundled JS is stored in memory, which allows for much faster recompilation on changes, but the main.js file won't be created. You can also access localhost:9111/webpack-dev-server/\<route\> for a live reload environment. 

If you want to compile main.js as a file, you can use `npm run compile` on its own and access the Play application directly.


**Client-side Principles**: See [https://github.com/guardian/membership-frontend/blob/master/docs/client-side-principles.md](https://github.com/guardian/membership-frontend/blob/master/docs/client-side-principles.md) for high-level client-side principles for Membership.

### Setup AWS credentials

Install the awscli:
```
brew install awscli
```

Fetch the developer AWS credentials, discuss how we do this with a team member.

### Setup NGINX

Follow the instructions in [`/nginx/README.md`](./nginx/README.md) in this project.

### Download private keys

Download our private keys from the `contributions-private` S3 bucket. If you have the AWS CLI set up you can run:

```
sudo aws s3 cp s3://contributions-private/DEV/contributions.private.conf /etc/gu/ --profile membership
```

**Update this when we have our own keys**

## Run

Start the app as follows:

```
sbt devrun
```

This will start:
 - the Play application on the port 9112
 - grunt watch to compile assets such as css
 - the webpack dev server on the port 9111, proxying calls to the play app for everything but js
  
Making a request to [http://localhost:9111](http://localhost:9111) should give you the homepage.
 

To make the site reachable as [https://contributions.thegulocal.com](https://contributions.thegulocal.com) (necessary for register/sign-in functionality) you then need to make sure NGINX is configured and running as described in [`/nginx/README.md`](./nginx/README.md).

## Testing

### Automated

#### JavaScript unit tests


```
npm test
```

#### Scala tests

```
sbt test
```

## Deployment

**TODO: Make this autodeployable**

We use continuous deployment of the `master` branch to Production (https://contribute.theguardian.com/).
See [fix-a-failed-deploy.md](https://github.com/guardian/deploy/blob/master/magenta-lib/docs/magenta-lib/howto/fix-a-failed-deploy.md)
for what to do if a deploy goes bad.

## Security

### Committing config credentials

For the Membership project, we put both `DEV` and `PROD` credentials in `contributions.private.conf` files in a private
S3 bucket, and if private credentials need adding or updating, they need to be updated there in S3.

You can download and update credentials like this

```
sudo aws s3 cp s3://contributions-private/DEV/contributions.private.conf /etc/gu/ --profile membership
sudo aws s3 cp /etc/gu/contributions.private.conf s3://contributions-private/DEV/ --profile membership
```

For a reminder on why we do this, here's @tackley on the subject:

>NEVER commit access keys, passwords or other sensitive credentials to any source code repository*.

>That's especially true for public repos, but also applies to any private repos. (Why? 1. it's easy to make it public and 2. every person who ever worked on your project almost certainly has a copy of your repo somewhere. It's too easy for a subsequently-disaffected individual to take advantage of this.)

>For AWS access keys, always prefer to use instance profiles instead.

>For other credentials, put them in a configuration store such as DynamoDB or a private S3 bucket.
