package com.marccortes.spaceInvaders;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.Color;

// Main game class for Space Invaders
public class MainGame extends ApplicationAdapter {
    // Constants
    private static final int UI_FONT_SCALE = 6;
    private static final float INVADER_SPEED_BOOST = 80;
    private static final float POWERUP_DROP_CHANCE = 0.06f;
    private static final float MULTI_SHOT_ACTIVE_TIME = 8f;
    private static final float SHIELD_ACTIVE_TIME = 8f;
    private static final float BLAST_DURATION = 0.8f;
    private static final float BOSS_DAMAGE_FEEDBACK_DURATION = 0.15f;
    private static final int STARTING_LIVES = 3;
    private static final int MAX_BOSS_HEALTH = 60;
    private static final int SCORE_PER_INVADER = 120;
    private static final int SCORE_PER_SHOOT_INVADER = 180;
    private static final int SCORE_PER_BOSS = 1200;

    // Game phases
    private enum GamePhase { MAIN_MENU, ACTIVE, PAUSED, SETTINGS, VICTORY, DEFEAT, FINAL_BOSS }
    private GamePhase currentPhase = GamePhase.MAIN_MENU;

    // UI and rendering
    private Stage gameStage;
    private Skin uiSkin;
    private SpriteBatch spriteRenderer;

    // Assets
    private Texture menuBackdrop, pauseScreen, spaceBackdrop;
    private Texture[] spaceshipTextures, invaderTextures, shootInvaderTextures, blastTextures;
    private Texture projectileTexture, powerupIcon, shieldIcon, bossSprite, laserTexture;
    private Animation<TextureRegion> spaceshipAnimation, invaderAnimation, shootInvaderAnimation, blastAnimation;
    private Music menuTrack, gameTrack;
    private Sound fireSound, lifeLostSound, invaderDestroyedSound, powerupCollectedSound, bossDamagedSound;
    private float musicLevel = 0.4f;
    private float soundLevel = 0.6f;

    // Game objects
    private static class Spaceship {
        float x, y, width = 140, height = 320;
        boolean isFiring = false;
        boolean multiShotActive = false;
        float multiShotDuration = 0f;
        boolean isShieldActive = false;
        float shieldDuration = 0f;
    }
    private Spaceship spaceship = new Spaceship();

    private static class Invader {
        Array<Rectangle> rectangles;
        Array<Boolean> isShootEnemy;
        float width = 80, height = 110;
        float startY;
        float moveSpeed = 180;
        int moveDirection = 1;
        float dropDistance = 70;
        float fireTimer = 0f;
        final float fireDelay = 0.75f; // Matches shoot_enemy animation (0.75s per frame)

        Invader() {
            rectangles = new Array<>();
            isShootEnemy = new Array<>();
        }
    }
    private Invader invader = new Invader(); // Initialize to prevent uninitialized variable error

    private static class Boss {
        Rectangle rectangle;
        float width = 320, height = 520;
        float moveSpeed = 320;
        int moveDirection = 1;
        int healthPoints = MAX_BOSS_HEALTH;
        boolean damageEffect = false;
        float damageTimer = 0f;
        boolean isLaserActive = false;
        float laserTimer = 0f;
        final float laserDelay = 3f;
        final float laserDuration = 0.5f;
        float laserWidth = 20, laserHeight = 600;
    }
    private Boss boss = new Boss();

    private static class Projectile {
        Array<Rectangle> rectangles = new Array<>();
        Array<Float> velocityX = new Array<>();
        Array<Float> velocityY = new Array<>();
        float width = 35, height = 30;
        float speed = 650;
        float fireDelay = 0.2f;
        float fireTimer = 0f;
    }
    private Projectile playerProjectiles = new Projectile();
    private Projectile enemyProjectiles = new Projectile();
    private final float enemyProjectileSpeed = 620;

    private static class Powerup {
        Array<Rectangle> rectangles = new Array<>();
        float width = 55, height = 55;
        float dropSpeed = 220;
    }
    private Powerup powerup = new Powerup();

    private static class Explosion {
        float x, y, timer;
        Explosion(float x, float y) {
            this.x = x;
            this.y = y;
            this.timer = 0f;
        }
    }
    private Array<Explosion> blasts = new Array<>();
    private float blastWidth = 150, blastHeight = 150;

    // Game state
    private float animationTime = 0f;
    private float gameTime = 0f;
    private float backdropYPosition = 0;
    private float backdropScrollSpeed = 60;
    private int playerScore = 0;
    private int playerLives = STARTING_LIVES;

    @Override
    public void create() {
        Gdx.app.log("MainGame", "Creating game");
        try {
            initializeUI();
            loadAssets();
            setupInput();
            displayMainMenu();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in create: " + e.getMessage(), e);
            throw e;
        }
    }

    private void initializeUI() {
        Gdx.app.log("MainGame", "Initializing UI");
        try {
            gameStage = new Stage(new ScreenViewport());
            uiSkin = new Skin(Gdx.files.internal("uiskin.json"));
            spriteRenderer = new SpriteBatch();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error initializing UI: " + e.getMessage(), e);
            throw e;
        }
    }

    private void loadAssets() {
        Gdx.app.log("MainGame", "Loading assets");
        try {
            // Audio
            menuTrack = Gdx.audio.newMusic(Gdx.files.internal("audio/menu_music.mp3"));
            gameTrack = Gdx.audio.newMusic(Gdx.files.internal("audio/game_music.mp3"));
            fireSound = Gdx.audio.newSound(Gdx.files.internal("audio/shoot.wav"));
            lifeLostSound = Gdx.audio.newSound(Gdx.files.internal("audio/life_lost.wav"));
            invaderDestroyedSound = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_killed.wav"));
            powerupCollectedSound = Gdx.audio.newSound(Gdx.files.internal("audio/powerup.wav"));
            bossDamagedSound = Gdx.audio.newSound(Gdx.files.internal("audio/enemy_killed.wav"));
            if (menuTrack == null || gameTrack == null || fireSound == null || lifeLostSound == null ||
                invaderDestroyedSound == null || powerupCollectedSound == null || bossDamagedSound == null) {
                throw new RuntimeException("Failed to load audio assets");
            }
            menuTrack.setLooping(true);
            menuTrack.setVolume(musicLevel);
            gameTrack.setLooping(true);
            gameTrack.setVolume(musicLevel);

            // Backgrounds
            menuBackdrop = new Texture("backgrounds/menu_background.png");
            pauseScreen = new Texture("backgrounds/pause_overlay.png");
            spaceBackdrop = new Texture("backgrounds/game_background.png");
            if (menuBackdrop == null || pauseScreen == null || spaceBackdrop == null) {
                throw new RuntimeException("Failed to load background textures");
            }

            // Sprites
            spaceshipTextures = new Texture[3];
            spaceshipTextures[0] = new Texture("sprites/player/00_player.png");
            spaceshipTextures[1] = new Texture("sprites/player/01_player.png");
            spaceshipTextures[2] = new Texture("sprites/player/02_player.png");
            for (Texture tex : spaceshipTextures) {
                if (tex == null) throw new RuntimeException("Failed to load spaceship textures");
            }

            invaderTextures = new Texture[2];
            invaderTextures[0] = new Texture("sprites/enemy/00_enemy.png");
            invaderTextures[1] = new Texture("sprites/enemy/01_enemy.png");
            for (Texture tex : invaderTextures) {
                if (tex == null) throw new RuntimeException("Failed to load invader textures");
            }

            shootInvaderTextures = new Texture[2];
            shootInvaderTextures[0] = new Texture("sprites/enemy/00_shoot_enemy.png");
            shootInvaderTextures[1] = new Texture("sprites/enemy/01_shoot_enemy.png");
            for (Texture tex : shootInvaderTextures) {
                if (tex == null) throw new RuntimeException("Failed to load shoot invader textures");
            }

            blastTextures = new Texture[8];
            for (int i = 0; i < 8; i++) {
                blastTextures[i] = new Texture(String.format("sprites/vfx/%02d_explosion.png", i));
                if (blastTextures[i] == null) throw new RuntimeException("Failed to load blast texture " + i);
            }

            projectileTexture = new Texture("sprites/player/bullet.png");
            powerupIcon = new Texture("sprites/powerup/triple_shot.png");
            shieldIcon = new Texture("sprites/powerup/shield_powerup.png");
            bossSprite = new Texture("sprites/enemy/boss.png");
            laserTexture = new Texture("sprites/enemy/laser.png");
            if (projectileTexture == null || powerupIcon == null || shieldIcon == null ||
                bossSprite == null || laserTexture == null) {
                throw new RuntimeException("Failed to load sprite textures");
            }

            // Animations
            Array<TextureRegion> frames = new Array<>();
            for (Texture tex : spaceshipTextures) frames.add(new TextureRegion(tex));
            spaceshipAnimation = new Animation<>(0.10f, frames, Animation.PlayMode.LOOP);
            frames.clear();
            for (Texture tex : invaderTextures) frames.add(new TextureRegion(tex));
            invaderAnimation = new Animation<>(0.75f, frames, Animation.PlayMode.LOOP);
            frames.clear();
            for (Texture tex : shootInvaderTextures) frames.add(new TextureRegion(tex));
            shootInvaderAnimation = new Animation<>(0.75f, frames, Animation.PlayMode.LOOP);
            frames.clear();
            for (Texture tex : blastTextures) frames.add(new TextureRegion(tex));
            blastAnimation = new Animation<>(BLAST_DURATION / 8, frames, Animation.PlayMode.NORMAL);
            if (spaceshipAnimation == null || invaderAnimation == null || shootInvaderAnimation == null || blastAnimation == null) {
                throw new RuntimeException("Failed to create animations");
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error loading assets: " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupInput() {
        Gdx.app.log("MainGame", "Setting up input");
        try {
            InputMultiplexer inputHandler = new InputMultiplexer();
            inputHandler.addProcessor(gameStage);
            Gdx.input.setInputProcessor(inputHandler);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error setting up input: " + e.getMessage(), e);
            throw e;
        }
    }

    private void initializeGame() {
        Gdx.app.log("MainGame", "Initializing game");
        try {
            // Reset state
            playerScore = 0;
            playerLives = STARTING_LIVES;
            invader.moveSpeed = 180;
            animationTime = 0f;
            gameTime = 0f;
            spaceship.isFiring = false;
            spaceship.multiShotActive = false;
            spaceship.multiShotDuration = 0f;
            spaceship.isShieldActive = false;
            spaceship.shieldDuration = 0f;
            boss.rectangle = null;
            boss.healthPoints = MAX_BOSS_HEALTH;
            boss.damageTimer = 0f;
            boss.damageEffect = false;
            invader.fireTimer = 0f;
            blasts.clear();
            powerup.rectangles.clear();
            playerProjectiles.rectangles.clear();
            playerProjectiles.velocityX.clear();
            playerProjectiles.velocityY.clear();
            enemyProjectiles.rectangles.clear();
            enemyProjectiles.velocityX.clear();
            enemyProjectiles.velocityY.clear();

            // Initialize spaceship
            Gdx.app.log("MainGame", "Setting up spaceship");
            spaceship.x = (Gdx.graphics.getWidth() - spaceship.width) / 2f;
            spaceship.y = Gdx.graphics.getHeight() * -0.03f;

            // Initialize invaders
            Gdx.app.log("MainGame", "Setting up invaders");
            invader.startY = Gdx.graphics.getHeight() - 150;
            invader.rectangles.clear();
            invader.isShootEnemy.clear();
            int totalInvaders = 28;
            int invadersPerRow = 7;
            int spacing = 60;
            int rows = (int) Math.ceil((float) totalInvaders / invadersPerRow);
            for (int row = 0; row < rows; row++) {
                int invadersInThisRow = Math.min(invadersPerRow, totalInvaders - row * invadersPerRow);
                float totalRowWidth = invadersInThisRow * invader.width + (invadersInThisRow - 1) * spacing;
                float startX = (Gdx.graphics.getWidth() - totalRowWidth) / 2f;
                float y = invader.startY - row * (invader.height + spacing);
                for (int col = 0; col < invadersInThisRow; col++) {
                    Rectangle rect = new Rectangle(startX + col * (invader.width + spacing), y, invader.width, invader.height);
                    invader.rectangles.add(rect);
                    invader.isShootEnemy.add(Math.random() < 0.2); // 20% chance to be shoot_enemy
                }
            }
            Gdx.app.log("MainGame", "Invaders initialized: " + invader.rectangles.size);

            // Setup UI
            Gdx.app.log("MainGame", "Setting up UI");
            gameStage.clear();
            createPauseButton();
            menuTrack.stop();
            gameTrack.play();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error initializing game: " + e.getMessage(), e);
            throw e;
        }
    }

    private void spawnFinalBoss() {
        Gdx.app.log("MainGame", "Spawning final boss");
        try {
            boss.rectangle = new Rectangle(
                (Gdx.graphics.getWidth() - boss.width) / 2f,
                Gdx.graphics.getHeight() - boss.height - 60,
                boss.width,
                boss.height
            );
            boss.healthPoints = MAX_BOSS_HEALTH;
            boss.moveDirection = 1;
            boss.damageTimer = 0f;
            boss.damageEffect = false;
            currentPhase = GamePhase.FINAL_BOSS;
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error spawning boss: " + e.getMessage(), e);
            throw e;
        }
    }

    private void updateSpaceshipPosition() {
        try {
            if (Gdx.input.isTouched()) {
                Gdx.app.log("MainGame", "Touch detected at x: " + Gdx.input.getX());
                spaceship.x = Gdx.input.getX() - spaceship.width / 2f;
                spaceship.x = Math.max(0, Math.min(spaceship.x, Gdx.graphics.getWidth() - spaceship.width));
                spaceship.isFiring = true;
            } else {
                spaceship.isFiring = false;
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error updating spaceship position: " + e.getMessage(), e);
        }
    }

    private void firePlayerProjectiles(float delta) {
        try {
            playerProjectiles.fireTimer += delta;
            if (spaceship.isFiring && playerProjectiles.fireTimer >= playerProjectiles.fireDelay) {
                Gdx.app.log("MainGame", "Firing player projectile");
                float centerX = spaceship.x + spaceship.width / 2f - playerProjectiles.width / 2f;
                float startY = spaceship.y + spaceship.height;
                fireSound.play(soundLevel);
                if (spaceship.multiShotActive) {
                    float[] angles = {90f, 100f, 80f};
                    for (float angleDeg : angles) {
                        float angleRad = (float) Math.toRadians(angleDeg);
                        Rectangle projectile = new Rectangle(centerX, startY, playerProjectiles.width, playerProjectiles.height);
                        playerProjectiles.rectangles.add(projectile);
                        playerProjectiles.velocityX.add((float) Math.cos(angleRad) * playerProjectiles.speed);
                        playerProjectiles.velocityY.add((float) Math.sin(angleRad) * playerProjectiles.speed);
                    }
                } else {
                    Rectangle projectile = new Rectangle(centerX, startY, playerProjectiles.width, playerProjectiles.height);
                    playerProjectiles.rectangles.add(projectile);
                    playerProjectiles.velocityX.add(0f);
                    playerProjectiles.velocityY.add(playerProjectiles.speed);
                }
                playerProjectiles.fireTimer = 0f;
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error firing player projectiles: " + e.getMessage(), e);
        }
    }

    private void fireEnemyProjectiles(float delta) {
        try {
            if (invader == null || invader.rectangles == null) {
                Gdx.app.log("MainGame", "Skipping enemy projectiles: invader or rectangles null");
                return;
            }
            invader.fireTimer += delta;
            if (invader.fireTimer >= invader.fireDelay) {
                Gdx.app.log("MainGame", "Firing enemy projectiles");
                for (int i = 0; i < invader.rectangles.size; i++) {
                    if (i >= invader.isShootEnemy.size) {
                        Gdx.app.error("MainGame", "Index out of bounds: isShootEnemy size=" + invader.isShootEnemy.size + ", i=" + i);
                        continue;
                    }
                    if (invader.isShootEnemy.get(i)) {
                        Rectangle invaderRect = invader.rectangles.get(i);
                        float centerX = invaderRect.x + invaderRect.width / 2f - enemyProjectiles.width / 2f;
                        float startY = invaderRect.y;
                        Rectangle projectile = new Rectangle(centerX, startY, enemyProjectiles.width, enemyProjectiles.height);
                        enemyProjectiles.rectangles.add(projectile);
                        enemyProjectiles.velocityX.add(0f);
                        enemyProjectiles.velocityY.add(-enemyProjectileSpeed);
                        fireSound.play(soundLevel);
                    }
                }
                invader.fireTimer = 0f;
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error firing enemy projectiles: " + e.getMessage(), e);
        }
    }

    private void updateGame(float delta) {
        try {
            Gdx.app.log("MainGame", "Updating game, phase: " + currentPhase);
            animationTime += delta;
            gameTime += delta;
            backdropYPosition -= backdropScrollSpeed * delta;
            if (backdropYPosition <= -Gdx.graphics.getHeight()) {
                backdropYPosition += Gdx.graphics.getHeight();
            }

            // Update spaceship
            updateSpaceshipPosition();

            // Update power-ups
            for (int i = powerup.rectangles.size - 1; i >= 0; i--) {
                Rectangle p = powerup.rectangles.get(i);
                p.y -= powerup.dropSpeed * delta;
                if (p.y + powerup.height < 0) {
                    powerup.rectangles.removeIndex(i);
                }
            }

            // Update blasts
            for (int i = blasts.size - 1; i >= 0; i--) {
                Explosion blast = blasts.get(i);
                blast.timer += delta;
                if (blast.timer >= BLAST_DURATION) {
                    blasts.removeIndex(i);
                }
            }

            // Update power-up timers
            if (spaceship.multiShotActive) {
                spaceship.multiShotDuration -= delta;
                if (spaceship.multiShotDuration <= 0) {
                    spaceship.multiShotActive = false;
                }
            }
            if (spaceship.isShieldActive) {
                spaceship.shieldDuration -= delta;
                if (spaceship.shieldDuration <= 0) {
                    spaceship.isShieldActive = false;
                }
            }

            if (currentPhase == GamePhase.ACTIVE) {
                updateActivePhase(delta);
            } else if (currentPhase == GamePhase.FINAL_BOSS) {
                updateFinalBossPhase(delta);
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error updating game: " + e.getMessage(), e);
        }
    }

    private void updateActivePhase(float delta) {
        try {
            if (invader == null || invader.rectangles == null) {
                Gdx.app.log("MainGame", "Skipping updateActivePhase: invader or rectangles null");
                return;
            }
            Gdx.app.log("MainGame", "Updating active phase, invaders: " + invader.rectangles.size);
            // Update invaders
            boolean shouldDrop = false;
            for (int i = 0; i < invader.rectangles.size; i++) {
                Rectangle inv = invader.rectangles.get(i);
                inv.x += invader.moveSpeed * invader.moveDirection * delta;
                if ((invader.moveDirection == 1 && inv.x + invader.width >= Gdx.graphics.getWidth()) ||
                    (invader.moveDirection == -1 && inv.x <= 0)) {
                    shouldDrop = true;
                }
            }
            if (shouldDrop) {
                invader.moveDirection *= -1;
                for (int i = 0; i < invader.rectangles.size; i++) {
                    Rectangle inv = invader.rectangles.get(i);
                    inv.y -= invader.dropDistance;
                }
            }

            // Update projectiles
            updateProjectiles(delta);

            // Fire projectiles
            firePlayerProjectiles(delta);
            fireEnemyProjectiles(delta);

            // Check collisions
            handleCollisionsActivePhase();

            // Spawn boss if all enemies defeated
            if (invader.rectangles.size == 0 && boss.rectangle == null) {
                spawnFinalBoss();
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in updateActivePhase: " + e.getMessage(), e);
        }
    }

    private void updateFinalBossPhase(float delta) {
        try {
            Gdx.app.log("MainGame", "Updating final boss phase");
            // Update boss
            if (boss.damageEffect) {
                boss.damageTimer -= delta;
                if (boss.damageTimer <= 0) {
                    boss.damageEffect = false;
                }
            }
            boss.rectangle.x += boss.moveSpeed * boss.moveDirection * delta;
            if (boss.rectangle.x + boss.width >= Gdx.graphics.getWidth() || boss.rectangle.x <= 0) {
                boss.moveDirection *= -1;
                boss.rectangle.x = Math.max(0, Math.min(boss.rectangle.x, Gdx.graphics.getWidth() - boss.width));
            }

            // Update laser
            boss.laserTimer += delta;
            if (boss.laserTimer >= boss.laserDelay) {
                boss.isLaserActive = true;
                boss.laserTimer = boss.laserDelay - boss.laserDuration;
            } else if (boss.laserTimer >= boss.laserDelay - boss.laserDuration) {
                boss.isLaserActive = false;
            }

            // Update projectiles
            updateProjectiles(delta);

            // Fire projectiles
            firePlayerProjectiles(delta);

            // Check collisions
            handleCollisionsFinalBossPhase();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in updateFinalBossPhase: " + e.getMessage(), e);
        }
    }

    private void updateProjectiles(float delta) {
        try {
            // Player projectiles
            for (int i = playerProjectiles.rectangles.size - 1; i >= 0; i--) {
                Rectangle p = playerProjectiles.rectangles.get(i);
                p.x += playerProjectiles.velocityX.get(i) * delta;
                p.y += playerProjectiles.velocityY.get(i) * delta;
                if (p.y > Gdx.graphics.getHeight() || p.x < -playerProjectiles.width || p.x > Gdx.graphics.getWidth()) {
                    playerProjectiles.rectangles.removeIndex(i);
                    playerProjectiles.velocityX.removeIndex(i);
                    playerProjectiles.velocityY.removeIndex(i);
                }
            }

            // Enemy projectiles
            for (int i = enemyProjectiles.rectangles.size - 1; i >= 0; i--) {
                Rectangle p = enemyProjectiles.rectangles.get(i);
                p.x += enemyProjectiles.velocityX.get(i) * delta;
                p.y += enemyProjectiles.velocityY.get(i) * delta;
                if (p.y < -enemyProjectiles.height || p.x < -enemyProjectiles.width || p.x > Gdx.graphics.getWidth()) {
                    enemyProjectiles.rectangles.removeIndex(i);
                    enemyProjectiles.velocityX.removeIndex(i);
                    playerProjectiles.velocityY.removeIndex(i);
                }
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error updating projectiles: " + e.getMessage(), e);
        }
    }

    private void handleCollisionsActivePhase() {
        try {
            if (invader == null || invader.rectangles == null) {
                Gdx.app.log("MainGame", "Skipping handleCollisionsActivePhase: invader or rectangles null");
                return;
            }
            Rectangle spaceshipRect = new Rectangle(spaceship.x, spaceship.y, spaceship.width, spaceship.height);

            // Invader collisions
            boolean invadersReachedBottom = false;
            for (int i = 0; i < invader.rectangles.size; i++) {
                Rectangle inv = invader.rectangles.get(i);
                if (inv.y <= spaceship.y + spaceship.height / 1.3) {
                    invadersReachedBottom = true;
                    break;
                }
            }
            if (invadersReachedBottom && !spaceship.isShieldActive) {
                loseLife();
                resetEnemies();
                return;
            }

            // Projectile collisions
            for (int i = playerProjectiles.rectangles.size - 1; i >= 0; i--) {
                Rectangle projectile = playerProjectiles.rectangles.get(i);
                for (int j = invader.rectangles.size - 1; j >= 0; j--) {
                    Rectangle inv = invader.rectangles.get(j);
                    if (projectile.overlaps(inv)) {
                        spawnBlast(inv.x + inv.width / 2f, inv.y + inv.height / 2f);
                        boolean isShootEnemy = invader.isShootEnemy.get(j);
                        invader.rectangles.removeIndex(j);
                        invader.isShootEnemy.removeIndex(j);
                        playerProjectiles.rectangles.removeIndex(i);
                        playerProjectiles.velocityX.removeIndex(i);
                        playerProjectiles.velocityY.removeIndex(i);
                        playerScore += isShootEnemy ? SCORE_PER_SHOOT_INVADER : SCORE_PER_INVADER;
                        invaderDestroyedSound.play(soundLevel);
                        spawnPowerup(inv);
                        break;
                    }
                }
            }

            // Power-up collisions
            for (int i = powerup.rectangles.size - 1; i >= 0; i--) {
                Rectangle p = powerup.rectangles.get(i);
                if (p.overlaps(spaceshipRect)) {
                    if (p.height == powerup.height) {
                        spaceship.multiShotActive = true;
                        spaceship.multiShotDuration = MULTI_SHOT_ACTIVE_TIME;
                    } else {
                        spaceship.isShieldActive = true;
                        spaceship.shieldDuration = SHIELD_ACTIVE_TIME;
                    }
                    powerupCollectedSound.play(soundLevel);
                    powerup.rectangles.removeIndex(i);
                }
            }

            // Enemy projectile collisions
            for (int i = enemyProjectiles.rectangles.size - 1; i >= 0; i--) {
                Rectangle projectile = enemyProjectiles.rectangles.get(i);
                if (projectile.overlaps(spaceshipRect) && !spaceship.isShieldActive) {
                    loseLife();
                    enemyProjectiles.rectangles.removeIndex(i);
                    enemyProjectiles.velocityX.removeIndex(i);
                    enemyProjectiles.velocityY.removeIndex(i);
                    return;
                }
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in handleCollisionsActivePhase: " + e.getMessage(), e);
        }
    }

    private void handleCollisionsFinalBossPhase() {
        try {
            Rectangle spaceshipRect = new Rectangle(spaceship.x, spaceship.y, spaceship.width, spaceship.height);

            // Boss collision
            if (boss.rectangle != null && boss.rectangle.y <= spaceship.y + spaceship.height / 1.3 && !spaceship.isShieldActive) {
                loseLife();
                resetBoss();
                return;
            }

            // Laser collision
            if (boss.isLaserActive && boss.rectangle != null) {
                Rectangle laserRect = new Rectangle(
                    boss.rectangle.x + boss.width / 2f - boss.laserWidth / 2f,
                    boss.rectangle.y - boss.laserHeight,
                    boss.laserWidth,
                    boss.laserHeight
                );
                if (laserRect.overlaps(spaceshipRect) && !spaceship.isShieldActive) {
                    loseLife();
                    resetBoss();
                    return;
                }
            }

            // Projectile collisions
            for (int i = playerProjectiles.rectangles.size - 1; i >= 0; i--) {
                Rectangle projectile = playerProjectiles.rectangles.get(i);
                if (boss.rectangle != null && projectile.overlaps(boss.rectangle)) {
                    spawnBlast(projectile.x + projectile.width / 2f, projectile.y + projectile.height / 2f);
                    boss.healthPoints--;
                    boss.damageEffect = true;
                    boss.damageTimer = BOSS_DAMAGE_FEEDBACK_DURATION;
                    playerProjectiles.rectangles.removeIndex(i);
                    playerProjectiles.velocityX.removeIndex(i);
                    playerProjectiles.velocityY.removeIndex(i);
                    bossDamagedSound.play(soundLevel);
                    if (boss.healthPoints <= 0) {
                        spawnBlast(boss.rectangle.x + boss.width / 2f, boss.rectangle.y + boss.height / 2f);
                        playerScore += SCORE_PER_BOSS;
                        boss.rectangle = null;
                        currentPhase = GamePhase.VICTORY;
                        playerProjectiles.rectangles.clear();
                        playerProjectiles.velocityX.clear();
                        playerProjectiles.velocityY.clear();
                        enemyProjectiles.rectangles.clear();
                        enemyProjectiles.velocityX.clear();
                        enemyProjectiles.velocityY.clear();
                        powerup.rectangles.clear();
                        Gdx.input.setInputProcessor(gameStage);
                        displayVictory();
                    }
                    break;
                }
            }

            // Power-up collisions
            for (int i = powerup.rectangles.size - 1; i >= 0; i--) {
                Rectangle p = powerup.rectangles.get(i);
                if (p.overlaps(spaceshipRect)) {
                    if (p.height == powerup.height) {
                        spaceship.multiShotActive = true;
                        spaceship.multiShotDuration = MULTI_SHOT_ACTIVE_TIME;
                    } else {
                        spaceship.isShieldActive = true;
                        spaceship.shieldDuration = SHIELD_ACTIVE_TIME;
                    }
                    powerupCollectedSound.play(soundLevel);
                    powerup.rectangles.removeIndex(i);
                }
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in handleCollisionsFinalBossPhase: " + e.getMessage(), e);
        }
    }

    private void loseLife() {
        try {
            playerLives--;
            lifeLostSound.play(soundLevel);
            if (playerLives <= 0) {
                currentPhase = GamePhase.DEFEAT;
                Gdx.input.setInputProcessor(gameStage);
                displayDefeat();
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in loseLife: " + e.getMessage(), e);
        }
    }

    private void resetEnemies() {
        try {
            if (invader == null) {
                invader = new Invader();
            }
            int totalInvaders = invader.rectangles.size;
            int invadersPerRow = 7;
            int spacing = 60;
            int rows = (int) Math.ceil((float) totalInvaders / invadersPerRow);
            invader.rectangles.clear();
            invader.isShootEnemy.clear();
            for (int row = 0; row < rows; row++) {
                int invadersInThisRow = Math.min(invadersPerRow, totalInvaders - row * invadersPerRow);
                float totalRowWidth = invadersInThisRow * invader.width + (invadersInThisRow - 1) * spacing;
                float startX = (Gdx.graphics.getWidth() - totalRowWidth) / 2f;
                float y = invader.startY - row * (invader.height + spacing);
                for (int col = 0; col < invadersInThisRow; col++) {
                    Rectangle rect = new Rectangle(startX + col * (invader.width + spacing), y, invader.width, invader.height);
                    invader.rectangles.add(rect);
                    invader.isShootEnemy.add(Math.random() < 0.2); // 20% chance to be shoot_enemy
                }
            }
            invader.moveSpeed += INVADER_SPEED_BOOST;
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error resetting enemies: " + e.getMessage(), e);
        }
    }

    private void resetBoss() {
        try {
            boss.rectangle.x = (Gdx.graphics.getWidth() - boss.width) / 2f;
            boss.rectangle.y = Gdx.graphics.getHeight() - boss.height - 60;
            boss.healthPoints = MAX_BOSS_HEALTH;
            boss.moveDirection = 1;
            enemyProjectiles.rectangles.clear();
            enemyProjectiles.velocityX.clear();
            enemyProjectiles.velocityY.clear();
            boss.damageTimer = 0f;
            boss.damageEffect = false;
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error resetting boss: " + e.getMessage(), e);
        }
    }

    private void spawnPowerup(Rectangle enemy) {
        try {
            if (Math.random() < POWERUP_DROP_CHANCE) {
                Rectangle p = new Rectangle();
                p.x = enemy.x + enemy.width / 2f - powerup.width / 2f;
                p.y = enemy.y;
                p.width = powerup.width;
                p.height = Math.random() < 0.5 ? powerup.height : powerup.height + 1;
                powerup.rectangles.add(p);
            }
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error spawning powerup: " + e.getMessage(), e);
        }
    }

    private void spawnBlast(float x, float y) {
        try {
            blasts.add(new Explosion(x - blastWidth / 2f, y - blastHeight / 2f));
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error spawning blast: " + e.getMessage(), e);
        }
    }

    private void renderGame() {
        try {
            Gdx.app.log("MainGame", "Rendering game");
            spriteRenderer.begin();
            spriteRenderer.draw(spaceBackdrop, 0, backdropYPosition, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            spriteRenderer.draw(spaceBackdrop, 0, backdropYPosition + Gdx.graphics.getHeight(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            for (Rectangle p : powerup.rectangles) {
                spriteRenderer.draw(p.height == powerup.height ? powerupIcon : shieldIcon, p.x, p.y, p.width, p.height);
            }

            for (Explosion blast : blasts) {
                float alpha = 1f - (blast.timer / BLAST_DURATION);
                spriteRenderer.setColor(1f, 1f, 1f, alpha);
                spriteRenderer.draw(blastAnimation.getKeyFrame(blast.timer, false), blast.x, blast.y, blastWidth, blastHeight);
                spriteRenderer.setColor(1f, 1f, 1f, 1f);
            }

            spriteRenderer.draw(spaceshipAnimation.getKeyFrame(animationTime, true), spaceship.x, spaceship.y, spaceship.width, spaceship.height);

            if (invader != null && invader.rectangles != null) {
                for (int i = 0; i < invader.rectangles.size; i++) {
                    if (i >= invader.isShootEnemy.size) {
                        Gdx.app.error("MainGame", "Index out of bounds in render: isShootEnemy size=" + invader.isShootEnemy.size + ", i=" + i);
                        continue;
                    }
                    Rectangle inv = invader.rectangles.get(i);
                    boolean isShootEnemy = invader.isShootEnemy.get(i);
                    if (isShootEnemy) {
                        TextureRegion frame = shootInvaderAnimation.getKeyFrame(animationTime, true);
                        int frameIndex = shootInvaderAnimation.getKeyFrameIndex(animationTime);
                        if (frameIndex == 1) { // 01_shoot_enemy.png needs flipping
                            TextureRegion flippedFrame = new TextureRegion(frame);
                            flippedFrame.flip(false, true); // Flip vertically
                            spriteRenderer.draw(flippedFrame, inv.x, inv.y, inv.width, inv.height);
                        } else {
                            spriteRenderer.draw(frame, inv.x, inv.y, inv.width, inv.height);
                        }
                    } else {
                        spriteRenderer.draw(invaderAnimation.getKeyFrame(animationTime, true), inv.x, inv.y, inv.width, inv.height);
                    }
                }
            }

            if (boss.rectangle != null) {
                if (boss.damageEffect) {
                    spriteRenderer.setColor(1f, 0.3f, 0.3f, 1f);
                }
                spriteRenderer.draw(bossSprite, boss.rectangle.x, boss.rectangle.y, boss.width, boss.height);
                spriteRenderer.setColor(1f, 1f, 1f, 1f);
                if (boss.isLaserActive) {
                    float laserX = boss.rectangle.x + boss.width / 2f - boss.laserWidth / 2f;
                    float laserY = boss.rectangle.y - boss.laserHeight;
                    spriteRenderer.draw(laserTexture, laserX, laserY, boss.laserWidth, boss.laserHeight);
                }
            }

            for (Rectangle p : playerProjectiles.rectangles) {
                spriteRenderer.draw(projectileTexture, p.x, p.y, p.width, p.height);
            }
            for (Rectangle p : enemyProjectiles.rectangles) {
                spriteRenderer.draw(projectileTexture, p.x, p.y, p.width, p.height);
            }

            spriteRenderer.end();

            gameStage.getBatch().begin();
            Label scoreDisplay = new Label("Score: " + playerScore, uiSkin);
            scoreDisplay.setFontScale(3);
            scoreDisplay.setPosition(30, Gdx.graphics.getHeight() - 60);
            scoreDisplay.draw(gameStage.getBatch(), 1);

            float livesX = Gdx.graphics.getWidth() - 250;
            float livesY = Gdx.graphics.getHeight() - 60;
            Label livesDisplay = new Label("Lives: " + playerLives, uiSkin);
            livesDisplay.setFontScale(3);
            livesDisplay.setPosition(livesX, livesY);
            livesDisplay.draw(gameStage.getBatch(), 1);

            if (spaceship.multiShotActive) {
                Label multiShotLabel = new Label("MULTI-SHOT!", uiSkin);
                multiShotLabel.setFontScale(3);
                multiShotLabel.setPosition(livesX, livesY - 50);
                multiShotLabel.draw(gameStage.getBatch(), 1);
            }
            if (spaceship.isShieldActive) {
                Label shieldLabel = new Label("SHIELD!", uiSkin);
                shieldLabel.setFontScale(3);
                shieldLabel.setPosition(livesX, livesY - (spaceship.multiShotActive ? 100 : 50));
                shieldLabel.draw(gameStage.getBatch(), 1);
            }
            if (currentPhase == GamePhase.FINAL_BOSS) {
                Label bossHealthDisplay = new Label("Boss HP: " + boss.healthPoints, uiSkin);
                bossHealthDisplay.setFontScale(3);
                bossHealthDisplay.setPosition(livesX - 50, livesY - (spaceship.multiShotActive ? 100 : 50));
                bossHealthDisplay.draw(gameStage.getBatch(), 1);
            }
            gameStage.getBatch().end();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error rendering game: " + e.getMessage(), e);
        }
    }

    @Override
    public void render() {
        try {
            float delta = Gdx.graphics.getDeltaTime();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            spriteRenderer.begin();
            if (currentPhase == GamePhase.MAIN_MENU || currentPhase == GamePhase.SETTINGS ||
                currentPhase == GamePhase.VICTORY || currentPhase == GamePhase.DEFEAT) {
                spriteRenderer.draw(menuBackdrop, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            } else if (currentPhase == GamePhase.PAUSED) {
                spriteRenderer.end();
                renderGame();
                spriteRenderer.begin();
                spriteRenderer.draw(pauseScreen, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
            spriteRenderer.end();

            if (currentPhase == GamePhase.ACTIVE || currentPhase == GamePhase.FINAL_BOSS) {
                updateGame(delta);
                renderGame();
            }

            gameStage.act(delta);
            gameStage.draw();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error in render: " + e.getMessage(), e);
        }
    }

    private void displayMainMenu() {
        try {
            Gdx.app.log("MainGame", "Displaying main menu");
            Gdx.input.setInputProcessor(gameStage);
            gameStage.clear();
            gameTrack.stop();
            menuTrack.play();

            Table menuTable = new Table();
            menuTable.setFillParent(true);
            menuTable.padTop(450);
            menuTable.setBackground(uiSkin.newDrawable("default-pane", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

            TextButton startGameButton = new TextButton("Start Game", uiSkin);
            startGameButton.getLabel().setFontScale(UI_FONT_SCALE);
            startGameButton.pad(20);
            TextButton settingsButton = new TextButton("Settings", uiSkin);
            settingsButton.getLabel().setFontScale(UI_FONT_SCALE);
            settingsButton.pad(20);

            startGameButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Start Game clicked");
                    currentPhase = GamePhase.ACTIVE;
                    backdropYPosition = 0;
                    powerupCollectedSound.play(soundLevel);
                    initializeGame();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    startGameButton.setColor(1, 1, 0.8f, 1);
                    startGameButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    startGameButton.setColor(1, 1, 1, 1);
                    startGameButton.setScale(1.0f);
                }
            });

            settingsButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Settings clicked");
                    currentPhase = GamePhase.SETTINGS;
                    powerupCollectedSound.play(soundLevel);
                    displaySettings();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    settingsButton.setColor(1, 1, 0.8f, 1);
                    settingsButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    settingsButton.setColor(1, 1, 1, 1);
                    settingsButton.setScale(1.0f);
                }
            });

            menuTable.add(startGameButton).width(600).height(100).padBottom(50).row();
            menuTable.add(settingsButton).width(600).height(100).row();
            gameStage.addActor(menuTable);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error displaying main menu: " + e.getMessage(), e);
        }
    }

    private void createPauseButton() {
        try {
            Gdx.app.log("MainGame", "Creating pause button");
            TextButton pauseButton = new TextButton("Pause", uiSkin);
            pauseButton.getLabel().setFontScale(2);
            pauseButton.pad(15);
            pauseButton.setSize(250, 60);
            pauseButton.setPosition(Gdx.graphics.getWidth() / 2f - pauseButton.getWidth() / 2f, Gdx.graphics.getHeight() - pauseButton.getHeight() - 20);
            pauseButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Pause button clicked");
                    currentPhase = GamePhase.PAUSED;
                    powerupCollectedSound.play(soundLevel);
                    displayPauseMenu();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    pauseButton.setColor(1, 1, 0.8f, 1);
                    pauseButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    pauseButton.setColor(1, 1, 1, 1);
                    pauseButton.setScale(1.0f);
                }
            });
            gameStage.addActor(pauseButton);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error creating pause button: " + e.getMessage(), e);
        }
    }

    private void displayPauseMenu() {
        try {
            Gdx.app.log("MainGame", "Displaying pause menu");
            if (currentPhase == GamePhase.ACTIVE || currentPhase == GamePhase.FINAL_BOSS) {
                gameTrack.pause();
            }
            gameStage.clear();
            Table pauseTable = new Table();
            pauseTable.setFillParent(true);
            pauseTable.padTop(100);
            pauseTable.setBackground(uiSkin.newDrawable("default-pane", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

            Label pauseTitle = new Label("Paused", uiSkin);
            pauseTitle.setFontScale(UI_FONT_SCALE);
            Label musicVolumeLabel = new Label("Music Volume", uiSkin);
            musicVolumeLabel.setFontScale(UI_FONT_SCALE);
            Slider musicVolumeSlider = new Slider(0f, 1f, 0.01f, false, uiSkin);
            musicVolumeSlider.setValue(musicLevel);
            musicVolumeSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    musicLevel = musicVolumeSlider.getValue();
                    menuTrack.setVolume(musicLevel);
                    gameTrack.setVolume(musicLevel);
                }
            });
            Label soundVolumeLabel = new Label("Sound Effects", uiSkin);
            soundVolumeLabel.setFontScale(UI_FONT_SCALE);
            Slider soundVolumeSlider = new Slider(0f, 1f, 0.01f, false, uiSkin);
            soundVolumeSlider.setValue(soundLevel);
            soundVolumeSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    soundLevel = soundVolumeSlider.getValue();
                }
            });
            TextButton resumeButton = new TextButton("Resume", uiSkin);
            resumeButton.getLabel().setFontScale(UI_FONT_SCALE);
            resumeButton.pad(20);
            TextButton mainMenuButton = new TextButton("Main Menu", uiSkin);
            mainMenuButton.getLabel().setFontScale(UI_FONT_SCALE);
            mainMenuButton.pad(20);

            resumeButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Resume button clicked");
                    currentPhase = (boss.rectangle != null && boss.healthPoints > 0) ? GamePhase.FINAL_BOSS : GamePhase.ACTIVE;
                    powerupCollectedSound.play(soundLevel);
                    gameTrack.play();
                    gameStage.clear();
                    createPauseButton();
                    setupInput();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    resumeButton.setColor(1, 1, 0.8f, 1);
                    resumeButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    resumeButton.setColor(1, 1, 1, 1);
                    resumeButton.setScale(1.0f);
                }
            });

            mainMenuButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Main Menu button clicked");
                    currentPhase = GamePhase.MAIN_MENU;
                    powerupCollectedSound.play(soundLevel);
                    playerProjectiles.rectangles.clear();
                    playerProjectiles.velocityX.clear();
                    playerProjectiles.velocityY.clear();
                    enemyProjectiles.rectangles.clear();
                    enemyProjectiles.velocityX.clear();
                    enemyProjectiles.velocityY.clear();
                    powerup.rectangles.clear();
                    blasts.clear();
                    boss.rectangle = null;
                    Gdx.input.setInputProcessor(gameStage);
                    displayMainMenu();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    mainMenuButton.setColor(1, 1, 0.8f, 1);
                    mainMenuButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    mainMenuButton.setColor(1, 1, 1, 1);
                    mainMenuButton.setScale(1.0f);
                }
            });

            pauseTable.add(pauseTitle).padBottom(50).row();
            pauseTable.add(musicVolumeLabel).padBottom(20).row();
            pauseTable.add(musicVolumeSlider).width(600).height(25).padBottom(25).row();
            pauseTable.add(soundVolumeLabel).padBottom(20).row();
            pauseTable.add(soundVolumeSlider).width(600).height(25).padBottom(25).row();
            pauseTable.add(resumeButton).width(600).height(100).padBottom(25).row();
            pauseTable.add(mainMenuButton).width(600).height(100).row();
            gameStage.addActor(pauseTable);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error displaying pause menu: " + e.getMessage(), e);
        }
    }

    private void displaySettings() {
        try {
            Gdx.app.log("MainGame", "Displaying settings");
            gameStage.clear();
            Table settingsTable = new Table();
            settingsTable.setFillParent(true);
            settingsTable.padTop(100);
            settingsTable.setBackground(uiSkin.newDrawable("default-pane", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

            Label settingsTitle = new Label("Settings", uiSkin);
            settingsTitle.setFontScale(UI_FONT_SCALE);
            Label musicVolumeLabel = new Label("Music Volume", uiSkin);
            musicVolumeLabel.setFontScale(UI_FONT_SCALE);
            Slider musicVolumeSlider = new Slider(0f, 1f, 0.01f, false, uiSkin);
            musicVolumeSlider.setValue(musicLevel);
            musicVolumeSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    musicLevel = musicVolumeSlider.getValue();
                    menuTrack.setVolume(musicLevel);
                    gameTrack.setVolume(musicLevel);
                }
            });
            Label soundVolumeLabel = new Label("Sound Effects", uiSkin);
            soundVolumeLabel.setFontScale(UI_FONT_SCALE);
            Slider soundVolumeSlider = new Slider(0f, 1f, 0.01f, false, uiSkin);
            soundVolumeSlider.setValue(soundLevel);
            soundVolumeSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    soundLevel = soundVolumeSlider.getValue();
                }
            });
            TextButton backButton = new TextButton("Back", uiSkin);
            backButton.getLabel().setFontScale(UI_FONT_SCALE);
            backButton.pad(20);

            backButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Settings back button clicked");
                    currentPhase = GamePhase.MAIN_MENU;
                    powerupCollectedSound.play(soundLevel);
                    displayMainMenu();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    backButton.setColor(1, 1, 0.8f, 1);
                    backButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    backButton.setColor(1, 1, 1, 1);
                    backButton.setScale(1.0f);
                }
            });

            settingsTable.add(settingsTitle).padBottom(75).row();
            settingsTable.add(musicVolumeLabel).padBottom(20).row();
            settingsTable.add(musicVolumeSlider).width(600).height(25).padBottom(25).row();
            settingsTable.add(soundVolumeLabel).padBottom(20).row();
            settingsTable.add(soundVolumeSlider).width(600).height(25).padBottom(25).row();
            settingsTable.add(backButton).width(600).height(100).row();
            gameStage.addActor(settingsTable);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error displaying settings: " + e.getMessage(), e);
        }
    }

    private void displayVictory() {
        try {
            Gdx.app.log("MainGame", "Displaying victory screen");
            spaceship.isFiring = false;
            spaceship.multiShotActive = false;
            spaceship.multiShotDuration = 0f;
            gameTrack.stop();
            menuTrack.play();
            gameStage.clear();

            Table victoryTable = new Table();
            victoryTable.setFillParent(true);
            victoryTable.padTop(200);
            victoryTable.setBackground(uiSkin.newDrawable("default-pane", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

            Label scoreLabel = new Label("Score: " + playerScore, uiSkin);
            scoreLabel.setFontScale(UI_FONT_SCALE);
            Label victoryLabel = new Label("Victory!", uiSkin);
            victoryLabel.setFontScale(UI_FONT_SCALE);
            TextButton backButton = new TextButton("Back", uiSkin);
            backButton.getLabel().setFontScale(UI_FONT_SCALE);
            backButton.pad(20);

            backButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Victory back button clicked");
                    currentPhase = GamePhase.MAIN_MENU;
                    powerupCollectedSound.play(soundLevel);
                    displayMainMenu();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    backButton.setColor(1, 1, 0.8f, 1);
                    backButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    backButton.setColor(1, 1, 1, 1);
                    backButton.setScale(1.0f);
                }
            });

            victoryTable.add(scoreLabel).padBottom(40).row();
            victoryTable.add(victoryLabel).padBottom(100).row();
            victoryTable.add(backButton).width(600).height(100).row();
            gameStage.addActor(victoryTable);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error displaying victory: " + e.getMessage(), e);
        }
    }

    private void displayDefeat() {
        try {
            Gdx.app.log("MainGame", "Displaying defeat screen");
            gameTrack.stop();
            menuTrack.play();
            Gdx.input.setInputProcessor(gameStage);
            gameStage.clear();

            Table defeatTable = new Table();
            defeatTable.setFillParent(true);
            defeatTable.padTop(200);
            defeatTable.setBackground(uiSkin.newDrawable("default-pane", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

            Label scoreLabel = new Label("Score: " + playerScore, uiSkin);
            scoreLabel.setFontScale(UI_FONT_SCALE);
            Label defeatLabel = new Label("Game Over!", uiSkin);
            defeatLabel.setFontScale(UI_FONT_SCALE);
            TextButton backButton = new TextButton("Back", uiSkin);
            backButton.getLabel().setFontScale(UI_FONT_SCALE);
            backButton.pad(20);

            backButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("MainGame", "Defeat back button clicked");
                    currentPhase = GamePhase.MAIN_MENU;
                    powerupCollectedSound.play(soundLevel);
                    displayMainMenu();
                }
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    backButton.setColor(1, 1, 0.8f, 1);
                    backButton.setScale(1.05f);
                }
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    backButton.setColor(1, 1, 1, 1);
                    backButton.setScale(1.0f);
                }
            });

            defeatTable.add(scoreLabel).padBottom(40).row();
            defeatTable.add(defeatLabel).padBottom(100).row();
            defeatTable.add(backButton).width(600).height(100).row();
            gameStage.addActor(defeatTable);
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error displaying defeat: " + e.getMessage(), e);
        }
    }

    @Override
    public void dispose() {
        try {
            Gdx.app.log("MainGame", "Disposing resources");
            gameStage.dispose();
            uiSkin.dispose();
            if (spriteRenderer != null) spriteRenderer.dispose();
            for (Texture tex : spaceshipTextures) if (tex != null) tex.dispose();
            for (Texture tex : invaderTextures) if (tex != null) tex.dispose();
            for (Texture tex : shootInvaderTextures) if (tex != null) tex.dispose();
            for (Texture tex : blastTextures) if (tex != null) tex.dispose();
            if (projectileTexture != null) projectileTexture.dispose();
            if (powerupIcon != null) powerupIcon.dispose();
            if (shieldIcon != null) shieldIcon.dispose();
            if (bossSprite != null) bossSprite.dispose();
            if (laserTexture != null) laserTexture.dispose();
            if (menuBackdrop != null) menuBackdrop.dispose();
            if (pauseScreen != null) pauseScreen.dispose();
            if (spaceBackdrop != null) spaceBackdrop.dispose();
            if (menuTrack != null) menuTrack.dispose();
            if (gameTrack != null) gameTrack.dispose();
            if (fireSound != null) fireSound.dispose();
            if (lifeLostSound != null) lifeLostSound.dispose();
            if (invaderDestroyedSound != null) invaderDestroyedSound.dispose();
            if (powerupCollectedSound != null) powerupCollectedSound.dispose();
            if (bossDamagedSound != null) bossDamagedSound.dispose();
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Error disposing resources: " + e.getMessage(), e);
        }
    }
}
