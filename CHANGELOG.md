# CHANGELOG
All notable changes to this project will be documented in this file.

## [Unreleased][unreleased]
- cancel the correct queued build (pull request #61)
- threads in parallel blocks now have names (pull request #54)
- remove comma in the "caused by" section (pull request #62)
- fix abort in parallel ignore blocks (pull request #64)
- links to builds now have a context menu (pull request #60)

## [0.18] - (released Jun. 10, 2015)
- display the tree of upstream causes (pull request #56)
- now depends on jenkins core **1.509.1** (LTS)

## [0.17] - (released Nov. 26, 2014)
- swap dependency with buildgraph-view (revert from 0.13, see #50).
- FlowCause now derives from UpstreamCause (pull request #53)

## [0.16] - (released Oct. 08, 2014)
- add syntax highlighting in DSL textarea (pull request #52)

## [0.15] - (released Sep. 10, 2014)
- fix missing displayName (pull request #51)

## [0.14] - (released Sep. 09, 2014)
- enable test-jar for plugins leveraging the extension point.
- use build.displayName in JobInvocation.toString (pull request #29)

## [0.13] - (released Sep. 09, 2014)
- read DSL from a file (pull request #47)
- fix buildgraph when using 2nd level flows (pull request #50)
- swap dependency with buildgraph-view (pull request #50)

## [0.12] - (released May 14, 2014)
- wait for build to be finalized
- fixed-width font in DSL box
- print stack traces when parallel builds fail
- restore ability to use a workspace, as a checkbox in flow configuration (useful for SCM using workspace)

## [0.11.1] - (released Apr. 08, 2014)
- no changes (added the compatibility warning to update center)

## [0.11] - (released Apr. 08, 2014)
- plugin re-licensed to MIT
- build flow no longer has a workspace
- Validation of the DSL when configuring the flow
- If a build could not be scheduled show the underlying cause
- extensions can contribute to dsl help
- aborting a flow causes all jobs scheduled and started by the flow to be aborted
- retry is configurable
- misc tweaks to UI and small fixes

## [0.10] - (released Aug. 08, 2013)
- add support for SimpleParameters (parameter that can be set from a String)
- mechanism to define DSL extensions
- visualization moved to build-graph-view plugin
- minor fixes

## [0.8] - (released Feb. 11, 2013)
- Fix folder support
- Basic flow visualization support (thanks to ~gregory144)
- Alternative map-style way to define parallel executions (thanks to Jeremy Voorhis)

## [0.7] - (released Jan. 11, 2013)
- Add support for ignore(Result)

## [0.6] - (released November 24, 2012)
- Enable "read job" permissions for Anonymous (issue #14027)
- Print errors as .. errors
- Better failed test reporting
- Use transient ref to Job/Build …
- Fix a NullPointer to render FlowCause after jenkins restart
- Use futures for synchronization plus publisher support plus console println cleanup (Pull request #14 from coreyoconnor)
- Call to Parametrized jobs correctly use their default values (Pull request #16 from dbaeli)

## [0.5] - (released September 03, 2012)
- fixed support for publishers (issue #14411)
- improved job configuration UI (dedicated section, help, prepare code mirror integration)
- improved error message

## [0.4] - (released June 28, 2012)
- fixed cast error parsing DSL (Collections$UnmodifiableRandomAccessList' to class 'long') on some version of Jenkins
- add groovy bindings for current build as "build", console output as "out", environment variables Map as "env", and triggered parameters of current build as "params"
- fixed bug when many "Parameters" links were shown for each triggered parameter on build page

## [0.3] - (released April 12, 2012)
add support for hierarchical project structure (typically : cloudbees folders plugin)

## [0.2] - (released April 09, 2012)
- changed parallel syntax to support nested flows concurrent execution
- fixed serialization issues

## [0.1] - (released April 03, 2012)
- initial release with DSL-based job orchestration


[unreleased]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.18...HEAD
[0.19]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.18...build-flow-plugin-0.19
[0.18]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.17...build-flow-plugin-0.18
[0.17]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.16...build-flow-plugin-0.17
[0.16]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.15...build-flow-plugin-0.16
[0.15]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.14...build-flow-plugin-0.15
[0.14]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.13...build-flow-plugin-0.14
[0.13]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.12...build-flow-plugin-0.13
[0.12]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.11...build-flow-plugin-0.12
[0.11.1]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.11...build-flow-plugin-0.11.1
[0.11]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.10...build-flow-plugin-0.11
[0.10]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.9...build-flow-plugin-0.10
[0.9]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.8...build-flow-plugin-0.9
[0.8]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.7...build-flow-plugin-0.8
[0.7]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.6...build-flow-plugin-0.7
[0.6]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.5...build-flow-plugin-0.6
[0.5]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.4...build-flow-plugin-0.5
[0.4]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.3...build-flow-plugin-0.4
[0.3]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.2...build-flow-plugin-0.3
[0.2]: https://github.com/jenkinsci/build-flow-plugin/compare/build-flow-plugin-0.1...build-flow-plugin-0.2
[0.1]: https://github.com/jenkinsci/build-flow-plugin/compare/385cb81801653e232cabfa302329c915e2a72c50...build-flow-plugin-0.1
