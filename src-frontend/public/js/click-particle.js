/* eslint-disable */
// 点击粒子特效 — 暖色系，匹配博客风格
(function () {
  // 暖色调色板：赤陶、琥珀、暖金、柔粉
  var COLORS = [
    "#c2594a", // terracotta
    "#cc8a5b", // warm amber
    "#d4a76a", // warm gold
    "#b5764a", // copper
    "#d4896b", // salmon clay
    "#e0b88a", // sand
    "#a94a3e", // deep terracotta
    "#c9957c", // rose clay
    "#d4a574", // wheat
    "#bf6b4a", // rust
  ];

  var canvas = document.createElement("canvas");
  canvas.style.cssText =
    "position:fixed;left:0;top:0;z-index:999999;pointer-events:none;";
  document.body.appendChild(canvas);

  var ctx = canvas.getContext("2d");
  var particles = [];
  var animId = null;
  var width, height;

  function resize() {
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = width * 2;
    canvas.height = height * 2;
    canvas.style.width = width + "px";
    canvas.style.height = height + "px";
    ctx.scale(2, 2);
  }

  resize();
  window.addEventListener("resize", function () {
    resize();
  });

  function Particle(x, y) {
    var angle = Math.random() * Math.PI * 2;
    var speed = 1.5 + Math.random() * 4;
    this.x = x;
    this.y = y;
    this.vx = Math.cos(angle) * speed;
    this.vy = Math.sin(angle) * speed;
    this.radius = 2 + Math.random() * 5;
    this.color = COLORS[Math.floor(Math.random() * COLORS.length)];
    this.alpha = 1;
    this.decay = 0.015 + Math.random() * 0.025;
    this.gravity = 0.04;
  }

  Particle.prototype.update = function () {
    this.vx *= 0.99;
    this.vy *= 0.99;
    this.vy += this.gravity;
    this.x += this.vx;
    this.y += this.vy;
    this.alpha -= this.decay;
    this.radius *= 0.998;
  };

  Particle.prototype.draw = function () {
    ctx.globalAlpha = this.alpha;
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
    ctx.fillStyle = this.color;
    ctx.fill();
    ctx.globalAlpha = 1;
  };

  function createBurst(x, y) {
    var count = 14 + Math.floor(Math.random() * 12);
    for (var i = 0; i < count; i++) {
      particles.push(new Particle(x, y));
    }
  }

  // 涟漪圈
  var ripples = [];

  function Ripple(x, y) {
    this.x = x;
    this.y = y;
    this.radius = 2;
    this.maxRadius = 40 + Math.random() * 30;
    this.alpha = 0.6;
    this.color = COLORS[Math.floor(Math.random() * COLORS.length)];
  }

  Ripple.prototype.update = function () {
    this.radius += 2;
    this.alpha -= 0.015;
  };

  Ripple.prototype.draw = function () {
    if (this.alpha <= 0) return;
    ctx.globalAlpha = this.alpha;
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
    ctx.strokeStyle = this.color;
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.globalAlpha = 1;
  };

  function onMouseDown(e) {
    // 忽略链接、图片、按钮等交互元素的点击
    var tag = e.target.nodeName;
    if (tag === "A" || tag === "IMG" || tag === "BUTTON" || tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || tag === "LABEL") return;
    if (e.target.closest("[data-no-particle]")) return;

    var x = e.clientX;
    var y = e.clientY;
    createBurst(x, y);
    ripples.push(new Ripple(x, y));

    if (!animId) {
      animId = requestAnimationFrame(loop);
    }
  }

  function loop() {
    ctx.clearRect(0, 0, width, height);

    for (var i = particles.length - 1; i >= 0; i--) {
      particles[i].update();
      particles[i].draw();
      if (particles[i].alpha <= 0 || particles[i].radius <= 0.3) {
        particles.splice(i, 1);
      }
    }

    for (var j = ripples.length - 1; j >= 0; j--) {
      ripples[j].update();
      ripples[j].draw();
      if (ripples[j].alpha <= 0 || ripples[j].radius >= ripples[j].maxRadius) {
        ripples.splice(j, 1);
      }
    }

    if (particles.length > 0 || ripples.length > 0) {
      animId = requestAnimationFrame(loop);
    } else {
      animId = null;
    }
  }

  document.addEventListener("mousedown", onMouseDown);
})();