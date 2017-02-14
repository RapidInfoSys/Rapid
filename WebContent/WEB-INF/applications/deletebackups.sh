#!/bin/bash

# find all directories called _backups and delete them and their contents
find . -name "_backups" -type d -exec rm -r "{}" \;
