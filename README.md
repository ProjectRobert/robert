# Project Robert

Open source Stormpath clone.

User Management as a Service.

The software is release under the [Eclipse Public License 1.0]

For a quick overview of what you can or cannot do [TL;DR-eclipse-license]

## Why ?

The point of the project is to provide an open source implementation of Stormpath.

I really like the idea of Stormpath, it is just a pain to write every time you build an web app a new management system.

There is a lot of boilerplate code, stuff like change password, recover account, email verification, user roles etc... that you don't really want to write every time again and again...

Also there is a security concerned, if you are not an expert about web security chances are that you management system is going to just work, without too many thoughts about security itself.

The idea of Stormpath is really great, they just do one, single things and they are great.

However I don't love the idea that the code of Stormpath is not open.

Here is why of Project Robert

## The main idea

The basic idea of Project Robert is too build a API that let developer manage user base very easily without too many concerned.

To establish a common ground I will use JSON-like as foundation, every user will be represented as a JSON document.

This will give you more that enough flexibility to build whatever user base you need, plus it is a wide established standard, and every developer know what JSON is.

For simplicity and velocity of development I will use MongoDB as persistent layer.

Every time a new application sign up I will create a new database in Mongo (for the SQL folks, databases in Mongo are comparable to tables).

Every new user of the application will go in that database, as simple as it sound.

Most common operation will be automated, stuff like change email or password or confirm a new email address.

Finally you can obviously query, change, and delete user using Robert as middle layer.

## Reason behind the technical choice

Here are the reason of the main technical choice behind the project, Clojure and Mongo.

### Robert is build in Clojure.

The choice of clojure is pretty obvious for me since it is my language of choice, however I would like to explain why I think (and hope) it is a good choice.

Clojure, as language, is very expressive and let you write very testable code, plus it help keep the complexity of the whole project as low as possible.

Clojure is still a young language, the community is so formed by very curious and smart people, always willing to learn new thing, the right people to push forward so big open source project as Robert.

### Robert is backed by MongoDB

Also the choice of Mongo was pretty obvious for me, but let me explain.

Mongo is a wide standard, and every developer know JSON.

Mongo is schema less, this provide the right flexibility to let every developer build its own user base without adding any code complexity to Robert itself.

Mongo let you interface with a storage system in a very easy and quick way.


## Money and Open Source, Robert as a Social Experiment

I am planning to sell the service, just like Stormpath does.

If and when I will get enough revenue to sustain myself I will work on the project full time. (~3000 €/months)

If and when the revenue will grow up, I will start to either reward the main contributors or hire someone full time.

Main contributors will have a discount on the use Robert itself.

All the code product by Project Robert will be Open Source.


## State of the project

May 24th:
* Migration from the old version of the MongoDB drivers that introduces some breaks
* Now every user/web-app has it's own database and not collection anymore.

May 1st:
* First README ready and some play code.

## Copyright and License

Copyright © 2014 Simone Mosciatti All right Reserved.

The use and distribution terms for this software are covered by the [Eclipse Public License 1.0] which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
[TL;DR-eclipse-license]: https://www.tldrlegal.com/l/epl
