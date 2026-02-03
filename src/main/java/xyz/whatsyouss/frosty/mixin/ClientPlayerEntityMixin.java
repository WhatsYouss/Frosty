package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.*;
import xyz.whatsyouss.frosty.utility.DirectionalInput;
import xyz.whatsyouss.frosty.utility.Rotations;
import xyz.whatsyouss.frosty.utility.Utils;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

	public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
		super(world, profile);
	}

	@Unique
	private PreMotionEvent preMotionEvent;

	@Shadow
	public Input input;

	@Shadow
	private double lastXClient;

	@Shadow
	private double lastYClient;

	@Shadow
	private double lastZClient;

	@Shadow
	private float lastYawClient;

	@Shadow
	private float lastPitchClient;

	@Shadow
	private int ticksSinceLastPositionPacketSent;

	@Shadow
	private boolean lastOnGround;

	@Shadow
	private boolean lastHorizontalCollision;

	@Shadow
	private boolean autoJumpEnabled;

	@Final
	@Shadow
    public ClientPlayNetworkHandler networkHandler;

	@Final
	@Shadow
	protected MinecraftClient client;

	@Shadow
    private void sendSprintingPacket() {}

	@Shadow
	protected boolean isCamera() { return true; }

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTickPre(CallbackInfo ci) {
		ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
		Frosty.EVENT_BUS.post(new PreUpdateEvent());
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTickPost(CallbackInfo ci) {
		ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
		World world = player.getEntityWorld();
		if (world.isChunkLoaded(new BlockPos((int) player.getX(), 0, (int) player.getZ()))) {
			Frosty.EVENT_BUS.post(new PostUpdateEvent());
		}
	}

	/**
	 * @author You
	 * @reason MoveFix
	 */
	@Overwrite
	private void sendMovementPackets() {
		ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
		Frosty.EVENT_BUS.post(PreSendMovementPacketsEvent.get());
		preMotionEvent = new PreMotionEvent(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.isOnGround(), player.isSprinting(), player.isSneaking());
		Frosty.EVENT_BUS.post(preMotionEvent);
		if (preMotionEvent.isCancelled()) {
			return;
		}
		this.sendSprintingPacket();
		if (this.isCamera()) {
			double d = preMotionEvent.getPosX() - this.lastXClient;
			double e = preMotionEvent.getPosY() - this.lastYClient;
			double f = preMotionEvent.getPosZ() - this.lastZClient;
			double g = (double)(preMotionEvent.getYaw() - this.lastYawClient);
			double h = (double)(preMotionEvent.getPitch() - this.lastPitchClient);
			++this.ticksSinceLastPositionPacketSent;
			boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
			boolean bl2 = g != (double)0.0F || h != (double)0.0F;
			if (bl && bl2) {
				this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(preMotionEvent.getPos(), preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround(), this.horizontalCollision));
				Rotations.lastServerYaw = preMotionEvent.getYaw();
				Rotations.lastServerPitch = preMotionEvent.getPitch();
			} else if (bl) {
				this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(preMotionEvent.getPos(), preMotionEvent.isOnGround(), this.horizontalCollision));
			} else if (bl2) {
				this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(preMotionEvent.getYaw(), preMotionEvent.getPitch(), preMotionEvent.isOnGround(), this.horizontalCollision));
				Rotations.lastServerYaw = preMotionEvent.getYaw();
				Rotations.lastServerPitch = preMotionEvent.getPitch();
			} else if (this.lastOnGround != preMotionEvent.isOnGround() || this.lastHorizontalCollision != this.horizontalCollision) {
				this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(preMotionEvent.isOnGround(), this.horizontalCollision));
			}

			if (bl) {
				this.lastXClient = preMotionEvent.getPosX();
				this.lastYClient = preMotionEvent.getPosY();
				this.lastZClient = preMotionEvent.getPosZ();
				this.ticksSinceLastPositionPacketSent = 0;
			}

			if (bl2) {
				this.lastYawClient = preMotionEvent.getYaw();
				this.lastPitchClient = preMotionEvent.getPitch();
			}

			this.lastOnGround = preMotionEvent.isOnGround();
			this.lastHorizontalCollision = this.horizontalCollision;
			this.autoJumpEnabled = (Boolean)this.client.options.getAutoJump().getValue();
		}

	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1))
	private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
		Frosty.EVENT_BUS.post(PreSendMovementPacketsEvent.get());
	}

	@Inject(method = "sendMovementPackets", at = @At("TAIL"))
	private void onSendMovementPacketsTail(CallbackInfo info) {
		Frosty.EVENT_BUS.post(PostSendMovementPacketsEvent.get());
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1, shift = At.Shift.AFTER))
	private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
		Frosty.EVENT_BUS.post(PostSendMovementPacketsEvent.get());
	}

	@ModifyVariable(
			method = "tickMovement",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/input/Input;tick()V",
					shift = At.Shift.AFTER
			),
			ordinal = 0
	)
	private boolean modifySprintInput(boolean original) {
		var event = new SprintEvent(
				new DirectionalInput(this.input.playerInput),
				original,
				SprintEvent.Source.INPUT
		);
		Frosty.EVENT_BUS.post(event);
		return event.getSprint();
	}

	@ModifyExpressionValue(
			method = "tickMovement",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/network/ClientPlayerEntity;canStartSprinting()Z"
			)
	)
	private boolean modifyCanSprint(boolean original) {
		var event = new SprintEvent(
				new DirectionalInput(this.input.playerInput),
				original,
				SprintEvent.Source.MOVEMENT_TICK
		);
		Frosty.EVENT_BUS.post(event);
		return event.getSprint();
	}

	@ModifyExpressionValue(
			method = "tickMovement",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldStopSprinting()Z"
			)
	)
	private boolean hookSprintStop(boolean original) {
		var event = new SprintEvent(
				new DirectionalInput(this.input.playerInput),
				!original,
				SprintEvent.Source.MOVEMENT_TICK
		);
		Frosty.EVENT_BUS.post(event);
		return !event.getSprint();
	}

	@ModifyExpressionValue(method = "sendSprintingPacket", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z")
	)
	private boolean hookNetworkSprint(boolean original) {
		var event = new SprintEvent(new DirectionalInput(input), original, SprintEvent.Source.NETWORK);
		Frosty.EVENT_BUS.post(event);
		return event.getSprint();
	}
}