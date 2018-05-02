#!/bin/bash
set -e # Exit with nonzero exit code if anything fails

SOURCE_BRANCH="master"
TARGET_BRANCH="gh-pages"

echo "$TRAVIS_REPO_SLUG $TRAVIS_JDK_VERSION $TRAVIS_PULL_REQUEST $TRAVIS_BRANCH"

if [ "$TRAVIS_REPO_SLUG" == "jontejj/jargo" ] && [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "$SOURCE_BRANCH" ]; then

    echo "Saving some useful information"
    REPO=`git config remote.origin.url`
    SSH_REPO=${REPO/https:\/\/github.com\//git@github.com:}
    SHA=`git rev-parse --verify HEAD`

    echo "Generating documentation for $REPO, SSH: $SSH_REPO"
    mvn --quiet "javadoc:javadoc"

    echo "Cloning the code for this repo"
    rm -rf $TARGET_BRANCH || exit 0

    echo "Setting up automatic ssh key"
    openssl aes-256-cbc -K $encrypted_5dfb394b8446_key -iv $encrypted_5dfb394b8446_iv -in id_rsa_travis.enc -out id_rsa_travis -d
    chmod 600 id_rsa_travis
    eval "$(ssh-agent -s)"
    ssh-add id_rsa_travis

    git clone $SSH_REPO $TARGET_BRANCH
    cd $TARGET_BRANCH
    # Create a new empty branch if $TARGET_BRANCH doesn't exist yet (should only happen on first deploy)
    git checkout $TARGET_BRANCH || git checkout --orphan $TARGET_BRANCH
    git config user.name "Travis CI"
    git config user.email "$COMMIT_AUTHOR_EMAIL"

    echo "Cleaning out old javadoc in repo"
    rm -rf javadoc/**/* || exit 0
    cd ..
    echo "Copying module javadocs"
    mvn --also-make dependency:tree | grep maven-dependency-plugin | awk 'NR>1 { print $(NF-1) }' | \
    while read module ; do \
    echo $module; done

    mvn --also-make dependency:tree | grep maven-dependency-plugin | awk 'NR>1 { print $(NF-1) }' | \
    while read module ; do \
    mkdir -p $TARGET_BRANCH/javadoc/$module
    cp -R $module/target/site/apidocs/* $TARGET_BRANCH/javadoc/$module/; done

    cd $TARGET_BRANCH

    if [ -z `git diff --exit-code` ]; then
        echo "No changes to the documentation on this push; exiting."
        exit 0
    fi

    echo "Commit the updated files"
    git add --all
    git commit -m "Deploy to GitHub Pages: ${SHA}"

    git push $SSH_REPO $TARGET_BRANCH

    echo -e "Update of documentation complete\n"
fi