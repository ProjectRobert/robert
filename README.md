# Project Robert

Open source Stormpath clone.

User Management as a Service.

The software is released under the [Eclipse Public License 1.0]

For a quick overview of what you can or cannot do [TL;DR-eclipse-license]

## Why ?

The point of the project is to provide an open source implementation of Stormpath.

I really like the idea of Stormpath, it is just a pain to write a new management system every time you build a web app.

There is a lot of boilerplate code, stuff like change password, recover account, email verification, user roles etc... that you don't really want to write every time over and over again...

Also there is a security concern, if you are not an expert about web security chances are that your management system is going to just work, without too many thoughts about security itself.

The idea of Stormpath works well, the team is great and highy specialised in it.

However I don't love the idea that the code of Stormpath is not open.

Here is the reason of Project Robert

## The main idea

The basic idea of Project Robert is to build an API that lets developers manage user base very easily without too many concerns.

To establish a common ground I will use JSON-like as foundation, every user will be represented as a JSON document.

This will give you more than enough flexibility to build whatever user base you need, plus it is a widly established standard, and all developers know what JSON is.

For simplicity and velocity of development I will use MongoDB as a persistent layer.

Every time a new application signs up I will create a new database in Mongo (for the SQL folks, databases in Mongo are comparable to tables).

Every new user of the application will go in that database, as simple as it sounds.

Most common operation will be automated, stuff like change email or password or confirm a new email address.

Finally you can obviously query, change, and delete user using Robert as middle layer.

## Documentation

The documentation of Robert can be found [here][doc]

## Pomodoros

I would really like to make this a big community effort, in order to do so any contributors need to know what is necessary to do next.

I will keep a lot of issues open, any issues will be very small, without the need to understand completely and deeply the whole project, a fun 30 minutes of code.

Those issues will be label as ["Pomodoro"][pomodoro].

Whoever can code some clojure should take less than 30 minutes to complete a "pomodoro" issues, otherwise it means that I wasn't able to manage the complexity of those issues.

Also I will keep a [file][pomodoro-file] of some other not-necessary-code-related quick issues to solve, it is meant to be an auto referential file, however any input is very welcome.

## Money and Open Source, Robert as a Social Experiment

I am planning to sell the service, just like Stormpath does.

If and when I will get enough revenue to sustain myself I will work on the project full time. (~3000 €/months)

If and when the revenue will grow up, I will start to either reward the main contributors or hire someone full time.

Main contributors will have a discount on the use Robert itself.

All the code produced by Project Robert will be Open Source.

## Support Robert, Hire me

I usually work for 100 $/hr, if you hire me trought Robert I will be very glad to work for 75 $/hr.

Out of that, 35 $/hr will be for my expense, the other 35 $/hr will go to as bounty on the Robert's Pomodoros.

## Reason behind the technical choice

Here are the reasons of the main technical choices behind the project, Clojure and Mongo.

### Robert is build in Clojure

The choice of clojure is pretty obvious for me since it is my language of choice, however I would like to explain why I think (and hope) it is a good choice.

Clojure, as language, is very expressive and allows you to write very testable code, plus it helps keep the complexity of the whole project as low as possible.

Clojure is still a young language, the community is made up of very curious and smart people, always willing to learn new things, the right people that would push forward such a big open source project as Robert.

### Robert is backed by MongoDB

Also the choice of Mongo was pretty obvious for me, but let me explain.

Mongo is a wide standard, and all developers know JSON.

Mongo is schema less, this provide the right flexibility to let every developer build its own user base without adding any code complexity to Robert itself.

Mongo let you interface with a storage system in a very easy and quick way.

## State of the project

May 24th:
* Migration from the old version of the MongoDB drivers that introduces some breaks
* Now every user/web-app has it's own database and not collection anymore.

May 1st:
* First README ready and some play code.

## Contributing

Everybody is more than welcome to contribute to the project.

Other than check the issues and the "pomodoris" for any task that can fit your skill.

If you are a developer you can just fork and build whatever features you need or feel like.

If you are a designer the project need a good looking home page, possible to be hosted on github.

If you know English better than myself (and you probably do) you can just hunt for the several mistake I did.

## Copyright and License

Copyright © 2014 Simone Mosciatti All right Reserved.

The use and distribution terms for this software are covered by the [Eclipse Public License 1.0] which can be found in the file epl-v10.html at the root of this distribution. By using this software in any fashion, you are agreeing to be bound by the terms of this license. You must not remove this notice, or any other, from this software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
[TL;DR-eclipse-license]: https://www.tldrlegal.com/l/epl
[pomodoro]: https://github.com/ProjectRobert/robert/issues?labels=Pomodoro&page=1&state=open
[pomodoro-file]: https://github.com/ProjectRobert/robert/blob/master/pomodoro.md
[doc]: http://docs.robert9.apiary.io/
