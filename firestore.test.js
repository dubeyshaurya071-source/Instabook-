const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

function makeUserProfile(uid, username, displayName) {
  const now = pastDate(10000);
  return {
    userId: uid,
    username,
    displayName,
    bio: "Creating videos on Instabook",
    avatarUrl: "",
    profileVisibility: "public",
    followersCount: 0,
    followingCount: 0,
    postsCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function makeVideoPost(authorId, username, displayName, visibility = "public") {
  const now = pastDate(8000);
  return {
    authorId,
    authorUsername: username,
    authorDisplayName: displayName,
    authorAvatarUrl: "",
    caption: "Blue sky vibes on Instabook! #instabook",
    videoUrl: "https://example.com/video.mp4",
    thumbnailUrl: "",
    category: "Trending",
    visibility,
    likesCount: 0,
    commentsCount: 0,
    sharesCount: 0,
    viewsCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any Instabook collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").where("profileVisibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("posts").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, "alice", "Alice"))
  );
  await assertFails(
    unauthDb.collection("posts").doc("post_1").set(makeVideoPost(ALICE_UID, "alice", "Alice"))
  );
});

test("UserProfile: owner can create/update profile, ghost fields and forged counters are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, "alice_v", "Alice"))
  );

  // Reject ghost field on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeUserProfile(BOB_UID, "bob_v", "Bob"),
      isAdmin: true,
    })
  );

  // Reject non-zero initial followersCount
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeUserProfile(BOB_UID, "bob_v", "Bob"),
      followersCount: 999,
    })
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated bio for Instabook",
      updatedAt: pastDate(1000),
    })
  );

  // Owner cannot directly inflate own followersCount without atomic follower doc
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      followersCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user profile edit rejected
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Public profiles list query matches Kotlin repository
  await assertSucceeds(
    bobDb.collection("users").where("profileVisibility", "==", "public").get()
  );

  // Unfiltered users list rejected
  await assertFails(bobDb.collection("users").get());
});

test("Follow/Subscribe: atomic follow & unfollow batch succeeds; non-atomic counter inflation fails", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(makeUserProfile(ALICE_UID, "alice", "Alice"));
    await db.collection("users").doc(BOB_UID).set(makeUserProfile(BOB_UID, "bob", "Bob"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Non-atomic counter bump on Alice must fail because /users/alice/followers/bob does not existAfter
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      followersCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Atomic follow batch (Bob follows Alice)
  const batch = bobDb.batch();
  const now = pastDate(1000);
  batch.set(
    bobDb.collection("users").doc(ALICE_UID).collection("followers").doc(BOB_UID),
    {
      userId: BOB_UID,
      targetUserId: ALICE_UID,
      username: "bob",
      displayName: "Bob",
      avatarUrl: "",
      profileVisibility: "public",
      createdAt: now,
    }
  );
  batch.set(
    bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID),
    {
      userId: ALICE_UID,
      targetUserId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      avatarUrl: "",
      profileVisibility: "public",
      createdAt: now,
    }
  );
  batch.update(bobDb.collection("users").doc(ALICE_UID), {
    followersCount: 1,
    updatedAt: now,
  });
  batch.update(bobDb.collection("users").doc(BOB_UID), {
    followingCount: 1,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Followers list query matches Kotlin repository
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("followers")
      .where("profileVisibility", "==", "public")
      .get()
  );

  // Atomic unfollow batch
  const unfollowBatch = bobDb.batch();
  const now2 = pastDate(500);
  unfollowBatch.delete(
    bobDb.collection("users").doc(ALICE_UID).collection("followers").doc(BOB_UID)
  );
  unfollowBatch.delete(
    bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID)
  );
  unfollowBatch.update(bobDb.collection("users").doc(ALICE_UID), {
    followersCount: 0,
    updatedAt: now2,
  });
  unfollowBatch.update(bobDb.collection("users").doc(BOB_UID), {
    followingCount: 0,
    updatedAt: now2,
  });
  await assertSucceeds(unfollowBatch.commit());
});

test("VideoPosts, Likes, Comments, and Shares enforce schema, ownership, and atomic likes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates public post and private post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_pub").set(makeVideoPost(ALICE_UID, "alice", "Alice", "public"))
  );
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_priv").set(makeVideoPost(ALICE_UID, "alice", "Alice", "private"))
  );

  // Bob cannot create post impersonating Alice
  await assertFails(
    bobDb.collection("posts").doc("post_fake").set(makeVideoPost(ALICE_UID, "alice", "Alice", "public"))
  );

  // Bob cannot include @DocumentId `id` key in post document
  await assertFails(
    bobDb.collection("posts").doc("post_id_trap").set({
      id: "post_id_trap",
      ...makeVideoPost(BOB_UID, "bob", "Bob", "public"),
    })
  );

  // Bob can query public posts, cannot read Alice's private post, cannot run unfiltered query
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("posts").doc("post_priv").get());
  await assertFails(bobDb.collection("posts").get());

  // Non-atomic like count bump fails
  await assertFails(
    bobDb.collection("posts").doc("post_pub").update({
      likesCount: 1,
      updatedAt: pastDate(500),
    })
  );

  // Atomic like batch succeeds
  const likeBatch = bobDb.batch();
  const now = pastDate(500);
  likeBatch.set(
    bobDb.collection("posts").doc("post_pub").collection("likes").doc(BOB_UID),
    {
      userId: BOB_UID,
      postId: "post_pub",
      createdAt: now,
    }
  );
  likeBatch.set(
    bobDb.collection("users").doc(BOB_UID).collection("likedPosts").doc("post_pub"),
    {
      userId: BOB_UID,
      postId: "post_pub",
      createdAt: now,
    }
  );
  likeBatch.update(bobDb.collection("posts").doc("post_pub"), {
    likesCount: 1,
    updatedAt: now,
  });
  await assertSucceeds(likeBatch.commit());

  // Comment create & query
  await assertSucceeds(
    bobDb.collection("posts").doc("post_pub").collection("comments").doc("c_1").set({
      postId: "post_pub",
      authorId: BOB_UID,
      authorUsername: "bob",
      authorDisplayName: "Bob",
      authorAvatarUrl: "",
      text: "Awesome video!",
      visibility: "public",
      createdAt: pastDate(400),
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("posts")
      .doc("post_pub")
      .collection("comments")
      .where("visibility", "==", "public")
      .get()
  );

  // Share increment succeeds
  await assertSucceeds(
    bobDb.collection("posts").doc("post_pub").update({
      sharesCount: 1,
      updatedAt: pastDate(300),
    })
  );

  // Charlie cannot delete Bob's comment or Alice's post
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("posts").doc("post_pub").collection("comments").doc("c_1").delete()
  );
  await assertFails(charlieDb.collection("posts").doc("post_pub").delete());
});

test("Real-time Notifications: actor can notify recipient, only recipient can read or mark seen", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Bob notifies Alice about a like
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_1").set({
      recipientId: ALICE_UID,
      actorId: BOB_UID,
      actorUsername: "bob",
      actorDisplayName: "Bob",
      actorAvatarUrl: "",
      type: "like",
      postId: "post_pub",
      message: "Bob liked your video",
      seen: false,
      createdAt: pastDate(500),
    })
  );

  // Bob cannot spoof actorId as Charlie
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_2").set({
      recipientId: ALICE_UID,
      actorId: CHARLIE_UID,
      actorUsername: "charlie",
      actorDisplayName: "Charlie",
      actorAvatarUrl: "",
      type: "like",
      postId: "post_pub",
      message: "Spoofed",
      seen: false,
      createdAt: pastDate(500),
    })
  );

  // Charlie cannot read Alice's notifications
  await assertFails(charlieDb.collection("users").doc(ALICE_UID).collection("notifications").get());

  // Alice can list and mark her notification seen
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_1").update({
      seen: true,
    })
  );
});
