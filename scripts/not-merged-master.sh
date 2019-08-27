#!/bin/sh
git checkout master
git reset --hard
git clean -fd
git pull
git remote prune origin
for branch in $(git branch -r --no-merged | grep -v HEAD);
do echo -e "$(git show --format="%ci | %an" "$branch" | head -n 1)" " | $branch";
done | sort -r > "$1"