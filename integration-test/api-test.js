// Runs the main API scenario against a running app.
// Usage: node integration-test/api-test.js [baseUrl]   (default http://localhost:8080)

const baseUrl = process.argv[2] || "http://localhost:8080";

let passed = 0;

function check(name, actual, expected) {
    if (actual === expected) {
        console.log("ok   " + name);
        passed++;
    } else {
        console.log("FAIL " + name + ": expected " + expected + ", got " + actual);
        process.exit(1);
    }
}

async function api(method, path, body, token) {
    const headers = {"Content-Type": "application/json"};
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }
    const res = await fetch(baseUrl + path, {
        method: method,
        headers: headers,
        body: body ? JSON.stringify(body) : undefined,
        redirect: "manual"
    });
    let json = null;
    try {
        json = await res.json();
    } catch (e) {
        // no body (204, redirects)
    }
    return {status: res.status, body: json, location: res.headers.get("location")};
}

async function main() {
    const email = "user" + Date.now() + "@test.com";
    const password = "password123";

    let res = await api("POST", "/api/v1/auth/register", {email, password});
    check("register", res.status, 201);
    res = await api("POST", "/api/v1/auth/register", {email, password});
    check("register same email again", res.status, 409);

    res = await api("POST", "/api/v1/auth/login", {email, password: "wrong"});
    check("login with wrong password", res.status, 401);
    res = await api("POST", "/api/v1/auth/login", {email, password});
    check("login", res.status, 200);
    const token = res.body.token;

    res = await api("GET", "/api/v1/users/me");
    check("me without token", res.status, 401);
    res = await api("GET", "/api/v1/users/me", null, token);
    check("me", res.body.email, email);
    const userId = res.body.id;

    res = await api("POST", "/api/v1/links", {originalUrl: "https://spring.io/projects/spring-boot"}, token);
    check("create link", res.status, 201);
    const link = res.body;
    console.log("     short url: " + link.shortUrl);

    const alias = "demo" + Date.now();
    res = await api("POST", "/api/v1/links", {originalUrl: "https://docs.spring.io/", alias: alias}, token);
    check("create link with alias", res.body.code, alias);
    res = await api("POST", "/api/v1/links", {originalUrl: "https://example.com", alias: alias}, token);
    check("alias already taken", res.status, 409);
    res = await api("POST", "/api/v1/links", {originalUrl: "not a url"}, token);
    check("invalid url", res.status, 400);

    res = await api("GET", "/" + link.code);
    check("redirect", res.status, 302);
    check("redirect location", res.location, link.originalUrl);
    await api("GET", "/" + link.code);
    res = await api("GET", "/nope123");
    check("unknown code", res.status, 404);

    res = await api("GET", "/api/v1/links/" + link.id + "/stats", null, token);
    check("stats: 2 clicks", res.body.totalClicks, 2);
    res = await api("GET", "/api/v1/links", null, token);
    check("list: 2 links", res.body.page.totalElements, 2);

    res = await api("PATCH", "/api/v1/links/" + link.id, {status: "DISABLED"}, token);
    check("disable link", res.body.status, "DISABLED");
    res = await api("GET", "/" + link.code);
    check("disabled link gives 410", res.status, 410);
    res = await api("PATCH", "/api/v1/links/" + link.id, {status: "ACTIVE"}, token);
    check("enable link", res.body.status, "ACTIVE");
    res = await api("GET", "/" + link.code);
    check("enabled link redirects again", res.status, 302);

    res = await api("GET", "/api/v1/admin/links", null, token);
    check("admin api for a user", res.status, 403);
    res = await api("POST", "/api/v1/auth/login", {email: "admin@example.com", password: "admin123"});
    check("admin login", res.status, 200);
    const admin = res.body.token;

    res = await api("GET", "/api/v1/admin/links?ownerId=" + userId, null, admin);
    check("admin: links of the user", res.body.page.totalElements, 2);
    res = await api("POST", "/api/v1/admin/links/" + link.id + "/block", null, admin);
    check("admin: block link", res.body.status, "BLOCKED");
    res = await api("GET", "/" + link.code);
    check("blocked link gives 410", res.status, 410);
    res = await api("PATCH", "/api/v1/links/" + link.id, {status: "ACTIVE"}, token);
    check("owner can't enable blocked link", res.status, 409);
    res = await api("POST", "/api/v1/admin/links/" + link.id + "/unblock", null, admin);
    check("admin: unblock link", res.body.status, "ACTIVE");
    res = await api("GET", "/api/v1/admin/stats", null, admin);
    check("admin: global stats", res.status, 200);
    console.log("     users: " + res.body.totalUsers + ", links: " + res.body.totalLinks + ", clicks: " + res.body.totalClicks);

    res = await api("DELETE", "/api/v1/links/" + link.id, null, token);
    check("delete link", res.status, 204);
    res = await api("GET", "/" + link.code);
    check("deleted link gives 404", res.status, 404);

    console.log("\n" + passed + " checks passed against " + baseUrl);
}

main().catch(e => {
    console.log("FAIL: " + e.message + " (is the app running on " + baseUrl + "?)");
    process.exit(1);
});
