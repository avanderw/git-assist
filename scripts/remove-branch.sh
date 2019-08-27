#!/bin/sh
printf "[31;1m"
git push origin --delete "$1"
#echo "Will remove branch $1"
printf "[0m"
