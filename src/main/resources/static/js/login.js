(function () {
  "use strict";

  var TOKEN_KEY = "jwt";

  var form = document.getElementById("login-form");
  var emailInput = document.getElementById("email");
  var passwordInput = document.getElementById("password");
  var submitButton = document.getElementById("submit");
  var messageEl = document.getElementById("message");

  function showMessage(text, type) {
    messageEl.textContent = text;
    messageEl.className = "message message--" + type;
    messageEl.setAttribute("role", type === "error" ? "alert" : "status");
    messageEl.hidden = false;
  }

  function clearMessage() {
    messageEl.hidden = true;
    messageEl.textContent = "";
    emailInput.removeAttribute("aria-invalid");
    passwordInput.removeAttribute("aria-invalid");
  }

  function setLoading(loading) {
    submitButton.disabled = loading;
    submitButton.textContent = loading ? "Signing in…" : "Sign in";
  }

  function errorMessageFrom(status, body) {
    if (body && body.errors && typeof body.errors === "object") {
      var parts = Object.keys(body.errors).map(function (field) {
        var msgs = [].concat(body.errors[field]);
        return field + " " + msgs.join(", ");
      });
      if (parts.length) return parts.join("; ");
    }
    if (status === 401 || status === 422) return "Invalid email or password.";
    return "Sign in failed (HTTP " + status + "). Please try again.";
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    clearMessage();

    var email = emailInput.value.trim();
    var password = passwordInput.value;

    if (!email || !password) {
      if (!email) emailInput.setAttribute("aria-invalid", "true");
      if (!password) passwordInput.setAttribute("aria-invalid", "true");
      showMessage("Please enter your email and password.", "error");
      return;
    }

    setLoading(true);

    fetch("/users/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ user: { email: email, password: password } })
    })
      .then(function (response) {
        return response
          .json()
          .catch(function () {
            return null;
          })
          .then(function (body) {
            return { ok: response.ok, status: response.status, body: body };
          });
      })
      .then(function (result) {
        if (result.ok && result.body && result.body.user && result.body.user.token) {
          localStorage.setItem(TOKEN_KEY, result.body.user.token);
          showMessage(
            "Signed in as " + (result.body.user.username || email) + ". Token saved.",
            "success"
          );
          passwordInput.value = "";
        } else {
          emailInput.setAttribute("aria-invalid", "true");
          passwordInput.setAttribute("aria-invalid", "true");
          showMessage(errorMessageFrom(result.status, result.body), "error");
        }
      })
      .catch(function () {
        showMessage("Unable to reach the server. Check your connection and try again.", "error");
      })
      .then(function () {
        setLoading(false);
      });
  });
})();
