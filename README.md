# Modified RPA Automatable Routines Discoverer
A tool to discover automatable routines for robotic process mining


This is a modified version of https://github.com/a-bosco/RPA_AutomatableRoutinesDiscoverer



## Changes / Modifications

- Added the file Extension.java: 
    1. Function that can combines the source attribute of actions after a branch if they are similar but not exactly the same
    2. Export function that creates a json file of the dafsa as an adjacency list and a json file for the actions and their attributes

- Modified the RpaDiscovery.java file:
    1. Added the functions of the extension in the workflow of the Automatable Routines Discoverer 
    2. fixed exceptions which could happen under certain circumstances



