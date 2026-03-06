/**
 * Login page JavaScript
 * Fix: Mobile tap responsiveness for login button (ADO Work Item #31)
 *
 * Root cause: Mobile browsers add a ~300ms delay to click events to
 * differentiate single taps from double-tap-to-zoom gestures. This causes
 * the login button to feel unresponsive, requiring multiple taps.
 *
 * Fixes applied:
 * 1. CSS touch-action: manipulation (in style.css) - primary fix
 * 2. Passive touchstart listener for immediate visual feedback
 * 3. Proper form submission via 'submit' event (not click on button)
 * 4. Debounce protection against accidental double-submissions
 * 5. Loading state management for clear user feedback
 */

(function () {
  "use strict";

  var API_BASE = "/users/login";
  var DEBOUNCE_MS = 1000;

  var form = document.getElementById("login-form");
  var emailInput = document.getElementById("email");
  var passwordInput = document.getElementById("password");
  var loginButton = document.getElementById("login-button");
  var errorContainer = document.getElementById("error-messages");

  var isSubmitting = false;

  /**
   * Provide immediate visual feedback on touch start.
   * This makes the button feel responsive even before the click event fires.
   * Using passive: true so it does not block scrolling.
   */
  loginButton.addEventListener(
    "touchstart",
    function () {
      if (!isSubmitting) {
        loginButton.classList.add("is-active");
      }
    },
    { passive: true }
  );

  loginButton.addEventListener(
    "touchend",
    function () {
      loginButton.classList.remove("is-active");
    },
    { passive: true }
  );

  loginButton.addEventListener(
    "touchcancel",
    function () {
      loginButton.classList.remove("is-active");
    },
    { passive: true }
  );

  /**
   * Handle form submission.
   * Using the form 'submit' event ensures both button taps and
   * keyboard Enter key trigger login consistently.
   */
  form.addEventListener("submit", function (event) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    var email = emailInput.value.trim();
    var password = passwordInput.value;

    if (!email || !password) {
      showErrors(["Email and password are required."]);
      return;
    }

    setLoadingState(true);
    hideErrors();

    var payload = JSON.stringify({
      user: {
        email: email,
        password: password,
      },
    });

    var xhr = new XMLHttpRequest();
    xhr.open("POST", API_BASE, true);
    xhr.setRequestHeader("Content-Type", "application/json");

    xhr.onreadystatechange = function () {
      if (xhr.readyState !== XMLHttpRequest.DONE) {
        return;
      }

      setLoadingState(false);

      if (xhr.status === 200) {
        try {
          var data = JSON.parse(xhr.responseText);
          if (data.user && data.user.token) {
            localStorage.setItem("jwtToken", data.user.token);
            window.location.href = "/";
          }
        } catch (e) {
          showErrors(["Unexpected response from server."]);
        }
      } else if (xhr.status === 422 || xhr.status === 403) {
        try {
          var errorData = JSON.parse(xhr.responseText);
          var messages = formatErrors(errorData);
          showErrors(messages);
        } catch (e) {
          showErrors(["Invalid email or password."]);
        }
      } else {
        showErrors(["Unable to connect. Please try again."]);
      }
    };

    xhr.onerror = function () {
      setLoadingState(false);
      showErrors(["Network error. Please check your connection."]);
    };

    xhr.timeout = 10000;
    xhr.ontimeout = function () {
      setLoadingState(false);
      showErrors(["Request timed out. Please try again."]);
    };

    xhr.send(payload);
  });

  /**
   * Toggle loading state on the submit button.
   * Disables the button and shows a spinner to prevent double-submission.
   */
  function setLoadingState(loading) {
    isSubmitting = loading;
    loginButton.disabled = loading;

    if (loading) {
      loginButton.classList.add("is-loading");
      loginButton.textContent = "Signing in";
      loginButton.setAttribute("aria-busy", "true");
    } else {
      loginButton.classList.remove("is-loading");
      loginButton.textContent = "Sign in";
      loginButton.setAttribute("aria-busy", "false");

      /* Debounce: prevent re-submission for a short period */
      setTimeout(function () {
        isSubmitting = false;
      }, DEBOUNCE_MS);
    }
  }

  function showErrors(messages) {
    errorContainer.hidden = false;
    var ul = document.createElement("ul");
    for (var i = 0; i < messages.length; i++) {
      var li = document.createElement("li");
      li.textContent = messages[i];
      ul.appendChild(li);
    }
    errorContainer.innerHTML = "";
    errorContainer.appendChild(ul);
  }

  function hideErrors() {
    errorContainer.hidden = true;
    errorContainer.innerHTML = "";
  }

  function formatErrors(data) {
    var messages = [];
    if (data && data.errors) {
      var errors = data.errors;
      for (var field in errors) {
        if (Object.prototype.hasOwnProperty.call(errors, field)) {
          var fieldErrors = errors[field];
          if (Array.isArray(fieldErrors)) {
            for (var i = 0; i < fieldErrors.length; i++) {
              messages.push(field + " " + fieldErrors[i]);
            }
          } else {
            messages.push(field + " " + fieldErrors);
          }
        }
      }
    }
    return messages.length > 0 ? messages : ["Invalid email or password."];
  }
})();
