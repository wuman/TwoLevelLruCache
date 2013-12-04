TwoLevelLruCache
================

![Feature Image](https://raw.github.com/wuman/TwoLevelLruCache/master/src/site/twolevellrucache_logo.png)

A two-level LRU cache composed of a smaller, first level `LruCache` in memory 
and a larger, second level `DiskLruCache`.

The keys must be of `String` type. The values must be convertible to and from
a byte stream using a `Converter`.


INCLUDING IN YOUR PROJECT
-------------------------

There are two ways to include TwoLevelLruCache in your projects:

1. You can download the released jar file in the [Downloads page](https://github.com/wuman/TwoLevelLruCache/downloads).
2. If you use Maven to build your project you can simply add a dependency to this library.

        <dependency>
            <groupId>com.wu-man</groupId>
            <artifactId>twolevellrucache</artifactId>
            <version>0.7.1</version>
        </dependency>


DEPENDENCY
----------

TwoLevelLruCache depends on the following libraries:

* [LruCache](http://developer.android.com/reference/android/support/v4/util/LruCache.html) by [Jesse Wilson](https://plus.google.com/106557483623231970995/about)
* [DiskLruCache](https://github.com/jakewharton/DiskLruCache) by [Google Android](https://plus.google.com/106557483623231970995/posts/Ein9QjNVzSL) and [Jake Wharton](http://jakewharton.com/)
* [Apache Commons IO](http://commons.apache.org/io/) (mainly for quietly closing streams, may be removed in the future)


CONTRIBUTE
----------

If you would like to contribute code to TwoLevelLruCache you can do so through 
GitHub by forking the repository and sending a pull request.


DEVELOPER
---------

* David Wu <david@wu-man.com> - <http://blog.wu-man.com>


LICENSE
-------

    Copyright 2012 David Wu
    Copyright 2011 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/wuman/twolevellrucache/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

