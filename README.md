# ~~Membership~~ Contributions Frontend


## Table of Contents

1. [Getting Started](#getting-started)
1. [Setup](#setup)
1. [Run](#run)
1. [Tests](#tests)
1. [Deployment](#deployment)
1. [Test Users](#test-users)
1. [Security](#security)
1. [Additional Documentation](#additional)

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

A ~~good~~ way to check everything is setup correctly is to run the tests

```
npm test
```

For development you'll also need the following commands:

**Compile assets**

```
npm run compile
```

**Watch files for changes**

```
npm run watch
```

**Client-side Principles**: See [client-side-principles.md](docs/client-side-principles.md) for high-level client-side principles for Membership.

### Setup NGINX

Follow the instructions in [`/nginx/README.md`](./nginx/README.md) in this project.

### Setup AWS credentials

Install the awscli:
```
brew install awscli
```

Fetch the developer AWS credentials, discuss how we do this with a team member.


### Download private keys

Download our private keys from the `contributions-private` S3 bucket. If you have the AWS CLI set up you can run:

```
sudo aws s3 cp s3://contributions-private/DEV/contributions.private.conf /etc/gu/ --profile membership
```

**Update this when we have our own keys**

## Run

Start the app as follows:

```
./start-frontend.sh
```

This will start the Play application, which usually listens on port `9111`. Making a request to `localhost:9111` should give you the homepage.

To make the site reachable as `contributions.thegulocal.com` (necessary for register/sign-in functionality) you then need to make sure NGINX is configured and running as described in [`/nginx/README.md`](./nginx/README.md).

## Testing

### Automated

#### JavaScript unit tests


```
cd frontend/
npm test
```


#### Scala unit tests

`sbt fast-test`

#### Acceptance tests

1. Run local membership-frontend: `sbt devrun`
2. Run local [idenity-frontend](https://github.com/guardian/identity-frontend): `sbt devrun`
3. `sbt acceptance-test`

These are browser driving Selenium tests.

#### All tests

`sbt test`


## Deployment

**TODO: Make this autodeployable**

We use continuous deployment of the `master` branch to Production (https://membership.theguardian.com/).
See [fix-a-failed-deploy.md](https://github.com/guardian/deploy/blob/master/magenta-lib/docs/magenta-lib/howto/fix-a-failed-deploy.md)
for what to do if a deploy goes bad.

## Security

### Committing config credentials

For the Membership project, we put both `DEV` and `PROD` credentials in `contributions.private.conf` files in a private
S3 bucket, and if private credentials need adding or updating, they need to be updated there in S3.

You can download and update credentials like this

    aws s3 cp s3://contributions-private/DEV/contributions.private.conf /etc/gu
    aws s3 cp /etc/gu/contributions.private.conf s3://contributions-private/DEV/

For a reminder on why we do this, here's @tackley on the subject:

>NEVER commit access keys, passwords or other sensitive credentials to any source code repository*.

>That's especially true for public repos, but also applies to any private repos. (Why? 1. it's easy to make it public and 2. every person who ever worked on your project almost certainly has a copy of your repo somewhere. It's too easy for a subsequently-disaffected individual to take advantage of this.)

>For AWS access keys, always prefer to use instance profiles instead.

>For other credentials, put them in a configuration store such as DynamoDB or a private S3 bucket.

<a name="additional"></a>


## Additional Documentation

Further documentation notes and useful items can be found in [docs](/docs).

- [Troubleshooting](docs/Troubleshooting.md) for information on common problems and how to fix them.
- [Building AMIs](docs/building-amis.md) for how to update our AMIs
