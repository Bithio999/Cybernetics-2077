#!/usr/bin/env python3
"""Generate the mechanical API backport; reviewed version-specific classes live in src/main/java.

Keep gameplay in the canonical source tree. Generated files are disposable build output.
"""
from pathlib import Path
import re, shutil, json, gzip, struct, sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = HERE / 'build/generated/port'
shutil.rmtree(OUT, ignore_errors=True)

def arguments(s, at):
    depth=1; start=at+1; parts=[]; i=start; quote=None; escape=False
    while i<len(s):
        c=s[i]
        if quote:
            if escape: escape=False
            elif c=='\\': escape=True
            elif c==quote: quote=None
        elif c in ('"',"'"): quote=c
        elif c in '([{': depth+=1
        elif c in ')]}':
            depth-=1
            if depth==0: parts.append(s[start:i]); return parts,i
        elif c==',' and depth==1: parts.append(s[start:i]); start=i+1
        i+=1
    raise ValueError('Unbalanced Java call')

def calls(s, name, transform):
    matches=list(re.finditer(r'\b'+re.escape(name)+r'\s*\(',s))
    for m in reversed(matches):
        at=s.index('(',m.start()); args,end=arguments(s,at)
        replacement=transform(args)
        if replacement is not None:s=s[:at+1]+replacement+s[end:]
    return s

def data_calls(s):
    for m in reversed(list(re.finditer(r'\.getData\((Cybernetics2077\.\w+|CityContent\.\w+)\)',s))):
        end=m.start();i=end-1
        while i>=0 and s[i].isspace():i-=1
        depth=0
        while i>=0:
            c=s[i]
            if c==')':depth+=1
            elif c=='(':
                if depth==0:break
                depth-=1
            elif depth==0 and not(c.isalnum() or c in '_.$'):break
            i-=1
        start=i+1;receiver=s[start:end].strip()
        s=s[:start]+'dev.cybernetics2077.port.PortData.get('+receiver+', '+m[1]+')'+s[m.end():]
    return s

def transform(s, path):
    s=s.replace('net.neoforged.neoforge','net.minecraftforge')
    s=s.replace('net.neoforged.fml','net.minecraftforge.fml')
    s=s.replace('net.neoforged.api','net.minecraftforge.api')
    s=s.replace('net.neoforged.bus.api','net.minecraftforge.eventbus.api')
    s=s.replace('import net.minecraftforge.fml.common.EventBusSubscriber;', 'import net.minecraftforge.fml.common.Mod.EventBusSubscriber;')
    s=s.replace('import net.minecraftforge.common.NeoForge;', 'import net.minecraftforge.common.MinecraftForge;').replace('NeoForge.EVENT_BUS','MinecraftForge.EVENT_BUS')
    s=s.replace('RegistryFriendlyByteBuf','FriendlyByteBuf')
    s=s.replace('net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(', 'new net.minecraft.resources.ResourceLocation(')
    s=s.replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(')
    s=s.replace('ResourceLocation.withDefaultNamespace(', 'new ResourceLocation(')
    s=s.replace('ResourceLocation.parse(', 'new ResourceLocation(')
    s=s.replace('Math.clamp(', 'dev.cybernetics2077.port.PortMath.clamp(')
    s=s.replace('.getFirst()', '.get(0)')
    s=s.replace('HolderLookup.Provider', 'net.minecraft.core.RegistryAccess')
    s=re.sub(r'public CompoundTag serializeNBT\(net\.minecraft\.core\.RegistryAccess \w+\)', 'public CompoundTag serializeNBT()',s)
    s=re.sub(r'public void deserializeNBT\(net\.minecraft\.core\.RegistryAccess \w+, CompoundTag (\w+)\)',r'public void deserializeNBT(CompoundTag \1)',s)
    s=calls(s,'serializeNBT',lambda a:'' if len(a)==1 and a[0].strip() else None)
    s=calls(s,'deserializeNBT',lambda a:a[-1] if len(a)==2 else None)
    s=s.replace('import net.minecraftforge.attachment.AttachmentType;', 'import dev.cybernetics2077.port.DataKey;')
    s=s.replace('AttachmentType<','DataKey<').replace('AttachmentType.serializable','DataKey.serializable')
    s=re.sub(r'  private static final DeferredRegister<DataKey<\?>> (ATTACHMENTS|DATA) =\s*DeferredRegister.create\(NeoForgeRegistries.ATTACHMENT_TYPES, [^;]+;\n','',s)
    s=s.replace('ATTACHMENTS.register(', 'dev.cybernetics2077.port.PortData.register(')
    s=s.replace('DATA.register(\n          "street_account"', 'dev.cybernetics2077.port.PortData.register(\n          "street_account"')
    s=s.replace('    ATTACHMENTS.register(bus);','').replace('    DATA.register(bus);','')
    # Methods added by NeoForge's entity extension are routed through a Forge capability owner.
    s=data_calls(s)
    s=s.replace('public Cybernetics2077(IEventBus bus, ModContainer container) {',
        'public Cybernetics2077() {\n    var bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();')
    s=s.replace('container.registerConfig(', 'net.minecraftforge.fml.ModLoadingContext.get().registerConfig(')
    s=s.replace('ModConfigSpec', 'ForgeConfigSpec')
    s=s.replace('import net.minecraftforge.fml.ModContainer;', '')
    s=s.replace('net.minecraftforge.common.extensions.IMenuTypeExtension', 'net.minecraftforge.common.extensions.IForgeMenuType')
    s=s.replace('.eyeHeight(1.62f)', '')
    s=s.replace('import net.minecraftforge.network.event.RegisterPayloadHandlersEvent;', 'import dev.cybernetics2077.port.PortPayloadHandlers;')
    s=s.replace('RegisterPayloadHandlersEvent', 'PortPayloadHandlers')
    s=s.replace('net.minecraftforge.network.PacketDistributor', 'dev.cybernetics2077.port.PortPayloadHandlers').replace('PacketDistributor.', 'PortPayloadHandlers.')
    s=s.replace('import net.minecraft.network.codec.StreamCodec;', 'import dev.cybernetics2077.port.PortStreamCodec;').replace('StreamCodec', 'PortStreamCodec').replace('PortPortStreamCodec','PortStreamCodec')
    s=s.replace('import net.minecraft.network.protocol.common.custom.CustomPacketPayload;', 'import dev.cybernetics2077.port.PortPayload;').replace('CustomPacketPayload','PortPayload')
    s=re.sub(r'new Type<>\((Cybernetics2077.id\("[^"\n]+"\))\)', lambda m:'new Type<>('+m[1]+', '+re.search(r'(?:record|class) (\w+)',s[:m.start()][s.rfind('public record',0,m.start()):])[1]+'.class)', s)
    s=s.replace('    bus.addListener(this::network);','    network(new PortPayloadHandlers());')
    s=s.replace('    bus.addListener(ScannerNetwork::register);','    ScannerNetwork.register(new PortPayloadHandlers());')
    s=s.replace('    bus.addListener(CyberwareNetwork::register);','    CyberwareNetwork.register(new PortPayloadHandlers());')
    s=re.sub(r'(\w+)\.setData\((Cybernetics2077\.\w+), (.*)\);',r'dev.cybernetics2077.port.PortData.set(\1, \2, \3);',s)
    s=s.replace('TooltipContext', 'net.minecraft.world.level.Level')
    s=s.replace('net.minecraftforge.event.tick.ClientTickEvent', 'net.minecraftforge.event.TickEvent.ClientTickEvent')
    for kind in ('Client','Player','Level'):
        s=s.replace(kind+'TickEvent.Post',kind+'TickEvent')
        s=re.sub(r'(public static void \w+\('+kind+r'TickEvent (\w+)\) \{)',r'\1\n    if (\2.phase != net.minecraftforge.event.TickEvent.Phase.END) return;',s)
        s=s.replace('net.minecraftforge.event.tick.'+kind+'TickEvent','net.minecraftforge.event.TickEvent.'+kind+'TickEvent')
    s=re.sub(r'(public static void \w+\(PlayerTickEvent e\) \{)(.*?)(?=\n  })',lambda m:m[1]+m[2].replace('e.getEntity()', 'e.player'),s,flags=re.S)
    if 'LevelTickEvent' in s:s=s.replace('event.getLevel()', 'event.level')
    s=s.replace('net.minecraftforge.event.tick.EntityTickEvent', 'dev.cybernetics2077.port.PortEntityTickEvent').replace('EntityTickEvent.', 'PortEntityTickEvent.').replace('PortPortEntity','PortEntity')
    s=s.replace('LivingDamageEvent.Pre','LivingDamageEvent')
    s=s.replace('LivingDamageEvent.Post','dev.cybernetics2077.port.PortDamagePost')
    s=s.replace('LivingIncomingDamageEvent','dev.cybernetics2077.port.PortIncomingDamage')
    s=s.replace('import net.minecraftforge.event.entity.living.dev.cybernetics2077.port.PortIncomingDamage;', '')
    s=s.replace('net.minecraftforge.event.entity.living.dev.cybernetics2077.port.PortIncomingDamage','dev.cybernetics2077.port.PortIncomingDamage')
    s=s.replace('FinalizeSpawnEvent','MobSpawnEvent.FinalizeSpawn')
    if 'LivingDamageEvent e' in s:s=s.replace('e.getNewDamage()', 'e.getAmount()').replace('e.setNewDamage(', 'e.setAmount(')
    s=re.sub(r'import net.minecraft.(?:core.component.DataComponents|world.item.component.(?:CustomData|ItemContainerContents)|world.item.alchemy.PotionContents);\n', '',s)
    s=s.replace('.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)', '')
    s=re.sub(r'(\w+)\.getOrDefault\(DataComponents.CUSTOM_DATA, CustomData.EMPTY\).copyTag\(\)',r'dev.cybernetics2077.port.PortItems.custom(\1)',s)
    s=re.sub(r'(\w+)\.set\(DataComponents.CUSTOM_DATA, CustomData.of\((\w+)\)\)',r'dev.cybernetics2077.port.PortItems.custom(\1, \2)',s)
    s=re.sub(r'(\w+)\.getOrDefault\(DataComponents.CONTAINER, ItemContainerContents.EMPTY\).copyInto\((\w+)\)',r'dev.cybernetics2077.port.PortItems.load(\1, \2)',s)
    s=re.sub(r'(\w+)\.set\(DataComponents.CONTAINER, ItemContainerContents.fromItems\((\w+)\)\)',r'dev.cybernetics2077.port.PortItems.save(\1, \2)',s)
    s=re.sub(r'stack\s*.getOrDefault\(DataComponents.POTION_CONTENTS, PotionContents.EMPTY\)\s*.getAllEffects\(\)', 'net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(stack)',s)
    if path.name=='CityPoolElement.java':
        s=s.replace('MapCodec<CityPoolElement>', 'com.mojang.serialization.Codec<CityPoolElement>').replace('RecordCodecBuilder.mapCodec(', 'RecordCodecBuilder.create(')
        s=s.replace('projectionCodec(),\n                      overrideLiquidSettingsCodec()', 'projectionCodec()')
        s=s.replace('StructureTemplatePool.Projection projection,\n      Optional<LiquidSettings> liquid','StructureTemplatePool.Projection projection')
        s=s.replace('super(template, processors, projection, liquid)', 'super(template, processors, projection)')
        s=s.replace('StructureTemplatePool.Projection.RIGID,\n        Optional.of(LiquidSettings.IGNORE_WATERLOGGING)', 'StructureTemplatePool.Projection.RIGID')
        s=s.replace('      LiquidSettings liquid,\n','').replace('        liquid,\n','')
    if path.name=='CityResident.java':s=s.replace('defineSynchedData(SynchedEntityData.Builder b)', 'defineSynchedData()').replace('super.defineSynchedData(b)', 'super.defineSynchedData()').replace('b.define(', 'entityData.define(')
    s=s.replace('import net.minecraft.client.resources.PlayerSkin;', '')
    s=s.replace('ByteBufferBuilder','BufferBuilder')
    s=re.sub(r'(\w+)\.getSkin\(\).model\(\) == PlayerSkin.Model.SLIM',r'\1.getModelName().equals("slim")',s)
    s=re.sub(r'(\w+)\.getSkin\(\).texture\(\)',r'\1.getSkinTextureLocation()',s)
    if path.name=='HologramRenderer.java':s=s.replace('var skin = player.getSkin();', 'var skin = player.getSkinTextureLocation();').replace('skin.model() == PlayerSkin.Model.SLIM','player.getModelName().equals("slim")').replace('skin.texture()', 'skin')
    if 'ClientTickEvent' in s:s='import net.minecraftforge.event.TickEvent.ClientTickEvent;\n'.join(s.split('\n',1))
    if path.name=='ClientHooks.java':
        s=s.replace('screens(RegisterMenuScreensEvent e)', 'screens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e)')
        s=s.replace('e.<dev.cybernetics2077.cyberware.CyberwareMenu, CyberwareScreen>register(', 'net.minecraft.client.gui.screens.MenuScreens.<dev.cybernetics2077.cyberware.CyberwareMenu, CyberwareScreen>register(')
        s=s.replace('e.<dev.cybernetics2077.bench.CyberneticsBenchMenu, CyberneticsBenchScreen>register(', 'net.minecraft.client.gui.screens.MenuScreens.<dev.cybernetics2077.bench.CyberneticsBenchMenu, CyberneticsBenchScreen>register(')
        # Only registration method, queue to the render thread as required by Forge.
        start=s.index('public static void screens(');end=s.index('    @SubscribeEvent',start)
        part=s[start:end].replace('e.register(', 'net.minecraft.client.gui.screens.MenuScreens.register(').replace('{\n','{\n      e.enqueueWork(()->{\n',1)
        close=part.rfind('    }');part=part[:close]+'      });\n'+part[close:];s=s[:start]+part+s[end:]
    if path.name=='TacZCompat.java':s=s.replace('ICancellableEvent','Event')
    s=s.replace('@net.minecraftforge.fml.common.EventBusSubscriber', '@net.minecraftforge.fml.common.Mod.EventBusSubscriber')
    s=calls(s,'withColor',lambda a:'style -> style.withColor('+a[0]+')' if len(a)==1 else None).replace('.withColor(style ->', '.withStyle(style ->')
    s=s.replace('ItemStack.STREAM_CODEC.decode(data)', 'data.readItem()').replace('ItemStack.STREAM_CODEC.encode(data, deck)', 'data.writeItem(deck)')
    s=s.replace('server.openMenu(', 'net.minecraftforge.network.NetworkHooks.openScreen(server, ')
    s=s.replace('public ServerPlayer player()', 'public net.minecraft.world.entity.player.Player player()')
    s=s.replace('dev.cybernetics2077.port.PortData.register(bus);','')
    s=s.replace('net.minecraft.core.Holder<Attribute>', 'Attribute').replace('Holder<Attribute>','Attribute')
    for new,old in [('ADD_MULTIPLIED_TOTAL','MULTIPLY_TOTAL'),('ADD_MULTIPLIED_BASE','MULTIPLY_BASE'),('ADD_VALUE','ADDITION')]:s=s.replace('AttributeModifier.Operation.'+new,'AttributeModifier.Operation.'+old)
    s=s.replace('.amount()', '.getAmount()').replace('.operation()', '.getOperation()')
    s=calls(s,'AttributeModifier',lambda a:'dev.cybernetics2077.port.PortAttributes.uuid('+a[0]+'), '+a[0]+'.toString(), '+', '.join(a[1:]) if len(a)==3 else None)
    for method in ('getModifier','removeModifier'):s=calls(s,method,lambda a:'dev.cybernetics2077.port.PortAttributes.uuid('+a[0]+')' if len(a)==1 else None)
    s=re.sub(r'([\w.]+)\.registryAccess\(\)',r'\1.level().registryAccess()',s)
    s=s.replace('level.level().registryAccess()', 'level.registryAccess()').replace('serverLevel.level().registryAccess()', 'serverLevel.registryAccess()')
    s=s.replace('igniteForSeconds(', 'setSecondsOnFire(').replace('SoundEvents.TRIDENT_THUNDER.value()', 'SoundEvents.TRIDENT_THUNDER')
    s=s.replace('getNewAboutToBeSetTarget()', 'getNewTarget()').replace('setNewAboutToBeSetTarget(', 'setNewTarget(')
    s=s.replace('e.isCriticalHit()', 'e.isVanillaCritical()').replace('e.setCriticalHit(true)', 'e.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW)')
    s=s.replace('e.getDamageMultiplier()', 'e.getDamageModifier()').replace('e.setDamageMultiplier(', 'e.setDamageModifier(')
    s=s.replace('effect.getEffect().value()', 'effect.getEffect()')
    s=re.sub(r'effect.is\((MobEffects.\w+)\)',r'(effect.getEffect() == \1)',s)
    s=s.replace('net.minecraftforge.common.DeferredSpawnEggItem','net.minecraftforge.common.ForgeSpawnEggItem')
    s=s.replace('          EntityType.BOGGED,\n','').replace(' || type == EntityType.BOGGED','')
    s=s.replace('s.uploads.get(0)', 's.uploads.getFirst()')
    if path.name=='VillageServices.java':s=s.replace('p.get(0)', 'p.getFirst()').replace('pair.get(0)', 'pair.getFirst()')
    s=re.sub(r'(\w+)\.getLast\(\)',r'\1.get(\1.size()-1)',s)
    if path.name in ('ClinicDirectory.java','TemporalScheduled.java'):
        s=re.sub(r'new SavedData.Factory<>\((\w+)::new, \1::load\)',r'\1::load, \1::new',s)
        s=re.sub(r'CompoundTag (\w+), net.minecraft.core.RegistryAccess \w+',r'CompoundTag \1',s)
    if path.name=='CityBlock.java':
        start=s.index('  private static final MapCodec');end=s.index('  public CityBlock(',start);s=s[:start]+s[end:]
        s=re.sub(r'  @Override\n  protected MapCodec<[^}]+}\n','',s)
    s=re.sub(r'protected (BlockState|VoxelShape|net.minecraft.world.phys.shapes.VoxelShape) (getShape|rotate|mirror)\(',r'public \1 \2(',s)
    s=s.replace('protected InteractionResult useWithoutItem(', 'public InteractionResult use(').replace('Player player, BlockHitResult hit)', 'Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit)')
    s=s.replace('e.getContainer().getPostAttackInvulnerabilityTicks()', 'e.getEntity().invulnerableTime')
    if path.name=='AugmentRuntime.java':s=s.replace('Attributes.SNEAKING_SPEED','Attributes.MOVEMENT_SPEED').replace('"implant_sneak",','"implant_sneak",')
    # Existing vanilla sneak handling is supplied separately instead of a 1.21-only attribute.
    if path.name=='AugmentRuntime.java':
        s=re.sub(r'    attribute\(\s*p,\s*Attributes.MOVEMENT_SPEED,\s*"implant_sneak",.*?\);', '',s,flags=re.S)
    s=s.replace('e.getPartialTick().getGameTimeDeltaPartialTick(false)', 'e.getPartialTick()')
    s=s.replace('.getTimer().getGameTimeDeltaPartialTick(false)', '.getFrameTime()').replace('.getTimer().getGameTimeDeltaTicks()', '.getDeltaFrameTime()')
    s=s.replace('e.getModelViewMatrix()', 'e.getPoseStack().last().pose()')
    s=s.replace('e.getRenderer(type)', 'e.getRenderer((EntityType) type)')
    # 1.20 requires explicit vertex completion; preserve per-vertex color/UV/normal order.
    s=re.sub(r'(\.addVertex\([^;]+?)(;)',r'\1.endVertex()\2',s,flags=re.S)
    s=calls(s,'setColor',lambda a:', '.join('('+a[0]+shift+') & 255' for shift in (' >> 16',' >> 8','',' >>> 24')) if len(a)==1 else None)
    for new,old in [('addVertex','vertex'),('setColor','color'),('setUv1','overlayCoords'),('setUv2','uv2'),('setUv','uv'),('setOverlay','overlayCoords'),('setLight','uv2'),('setNormal','normal')]:s=s.replace(new,old)
    s=calls(s,'vertex',lambda a:a[0]+'.pose(), '+', '.join(a[1:]) if len(a)==4 and ('last()' in a[0] or a[0].strip()=='m') else None)
    s=calls(s,'normal',lambda a:a[0]+'.normal(), '+', '.join(a[1:]) if len(a)==4 else None)
    # Packed ARGB became four integer channels on the old vertex API.
    s=calls(s,'renderToBuffer',lambda a:', '.join(a[:4])+', '+', '.join('((('+a[4]+')'+shift+') & 255)/255f' for shift in (' >> 16',' >> 8','',' >>> 24')) if len(a)==5 else None)
    s=calls(s,'compile',lambda a:', '.join(a[:4])+', 1, 1, 1, 1' if len(a)==5 else None)
    s=s.replace('.buildOrThrow()', '.end()')
    s=re.sub(r'Tesselator.getInstance\(\).begin\(([^;]+?)\)',r'dev.cybernetics2077.port.PortRender.begin(\1)',s)
    s=s.replace('new BufferBuilder(MEMORY, batch.mode(), DefaultVertexFormat.NEW_ENTITY)', 'dev.cybernetics2077.port.PortRender.begin(MEMORY, batch.mode(), DefaultVertexFormat.NEW_ENTITY)')
    s=s.replace('.pushMatrix()', '.pushPose()').replace('.popMatrix()', '.popPose()')
    s=s.replace('mv.identity()', 'mv.setIdentity()').replace('mv.set(view)', 'mv.last().pose().set(view)')
    s=s.replace('modelView.set(RenderSystem.getModelViewStack())','modelView.set(RenderSystem.getModelViewStack().last().pose())')
    if path.name in ('PoseMesh.java','NativeArmSkin.java'):
        s=s.replace('vertex(float x, float y, float z)', 'vertex(double x, double y, double z)')
        s=s.replace('this.x = x', 'this.x = (float)x').replace('this.y = y', 'this.y = (float)y').replace('this.z = z', 'this.z = (float)z')
        at=s.index(' implements VertexConsumer {')+len(' implements VertexConsumer {')
        s=s[:at]+'\n    public void endVertex(){}\n    public void defaultColor(int r,int g,int b,int a){}\n    public void unsetDefaultColor(){}\n'+s[at:]
    s=s.replace('s.uploads.get(uploads.size()-1)', 's.uploads.getLast()')
    s=re.sub(r'owner\s*.registryAccess\(\)', 'owner.level().registryAccess()',s)
    s=re.sub(r'([\w.]+)\.getSkin\(\).model\(\)\s*== (?:net.minecraft.client.resources.)?PlayerSkin.Model.SLIM',r'\1.getModelName().equals("slim")',s)
    # Matrix pose multiplication on 1.20 does not update normals automatically.
    s=re.sub(r'(\w+)\.mulPose\((Mantis(?:Articulation|Rig)\.[^;]+|transform)\);',r'dev.cybernetics2077.port.PortRender.multiply(\1, \2);',s)
    s=s.replace('.uv2mapState(', '.setLightmapState(').replace('.overlayCoordsState(', '.setOverlayState(')
    if path.name=='CyberCanvas.java':s=re.sub(r'v.vertex\(p,', 'v.vertex(p.pose(),',s)
    if path.name=='HologramRenderer.java':
        s=calls(s,'render',lambda a:', '.join(a[:4])+', '+', '.join('((('+a[4]+')'+shift+') & 255)/255f' for shift in (' >> 16',' >> 8','',' >>> 24')) if len(a)==5 else None)
    s=re.sub(r'Tesselator.getInstance\(\)\s*.begin\(', 'dev.cybernetics2077.port.PortRender.begin(',s)
    s=s.replace('getScrollDeltaY()', 'getScrollDelta()').replace('m.levelRenderer.requestOutlineEffect();', '')
    s=s.replace('e.setCanRender(net.minecraftforge.common.util.TriState.FALSE)', 'e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY)')
    if path.name=='EnhancedMobLayer.java':s=s.replace('if (e.getRenderer((EntityType) type) instanceof LivingEntityRenderer renderer)', 'if (e.getRenderer((EntityType) type) != null)').replace('renderer.addLayer(new EnhancedMobLayer(renderer,', '((LivingEntityRenderer)e.getRenderer((EntityType)type)).addLayer(new EnhancedMobLayer((LivingEntityRenderer)e.getRenderer((EntityType)type),')
    if path.name=='AnimatedAtlas.java':s=s.replace('atlas.getTextures()', '((dev.cybernetics2077.mixin.client.PortAtlasAccess)(Object)atlas).cybernetics$sprites()')
    if path.name=='TemporalParticlesMixin.java':s=re.sub(r'"render\(Lnet/minecraft/client/renderer/LightTexture;[^"\n]+"','"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"',s)
    if path.name=='EnhancedMobLayer.java':
        s=s.replace('          renderer.addLayer(', '          ((LivingEntityRenderer)e.getRenderer((EntityType)type)).addLayer(').replace('                  renderer,','                  (LivingEntityRenderer)e.getRenderer((EntityType)type),')
    s=re.sub(r'(\w+)\.mulPose\(\s*(Mantis(?:Rig|View)\.[^;]+)\);',r'dev.cybernetics2077.port.PortRender.multiply(\1, \2);',s)
    if path.name=='AnimatedAtlas.java':s=re.sub(r'c.metadata\(\)\s*.getSection\(AnimationMetadataSection.SERIALIZER\)\s*.orElse\(AnimationMetadataSection.EMPTY\)', '((dev.cybernetics2077.port.PortSpriteMetadata)(Object)c).cybernetics$metadata()',s)
    if path.name=='CyberneticsBenchMenu.java':
        start=s.index('    return new Slot(inv, index, x, y) {');end=s.index('\n  public static int x(',start)
        s=s[:start]+'    return new dev.cybernetics2077.port.PortSlot(inv,index,x,y,this::installationActive);\n  }\n'+s[end:]
    if path.name in ('TemporalServerMixin.java','TemporalClientMixin.java'):
        s=s.replace('() -> original.call(entity)', '() -> { net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new dev.cybernetics2077.port.PortEntityTickEvent.Pre(entity)); original.call(entity); net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new dev.cybernetics2077.port.PortEntityTickEvent.Post(entity)); }')
    if path.name=='AugmentRuntime.java':s=s.replace('tier(p, LYNX_PAWS) * .04,', 'p.isShiftKeyDown() ? tier(p, LYNX_PAWS) * .04 : 0,')
    if str(path).endswith('TacZGameTests.java'):s=s.replace('ICancellableEvent','Event')
    s=calls(s,'FriendlyByteBuf',lambda a:a[0] if len(a)==2 else None)
    return s

sources=list((ROOT/'src/main/java').rglob('*.java'))
if '--tests' in sys.argv:sources+=list((ROOT/'compat/gametest').rglob('*.java'))
for path in sources:
    relative=path.relative_to(ROOT/'src/main/java') if path.is_relative_to(ROOT/'src/main/java') else Path('dev/cybernetics2077/compat/review')/path.name
    if (HERE/'src/main/java'/relative).exists():continue
    target=OUT/'java'/relative;target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(transform(path.read_text(),relative),encoding='utf-8')
for path in (ROOT/'src/main/resources').rglob('*'):
    if not path.is_file():continue
    relative=path.relative_to(ROOT/'src/main/resources')
    if str(relative)=='META-INF/neoforge.mods.toml':continue
    parts=list(relative.parts)
    for i,name in enumerate(parts):
        if i>0 and parts[i-1]=='tags' and name in ('block','item','fluid','entity_type','game_event'):parts[i]=name+'s'
        if name in ('recipe','loot_table','structure','advancement','item_modifier','predicate'):
            parts[i]=name+'s'
    target=OUT/'resources'/Path(*parts);target.parent.mkdir(parents=True,exist_ok=True)
    if target.name.endswith('.mixins.json'):
        data=json.loads(path.read_text());data['compatibilityLevel']='JAVA_17';data['minVersion']='0.8.5';data['refmap']='cybernetics2077.refmap.json';data.setdefault('mixins',[]).extend(['PortDamageMixin'] if target.name=='cybernetics2077.common.mixins.json' else []);data.setdefault('client',[]).extend(['PortAtlasAccess','PortSpriteMetadataMixin'] if target.name=='cybernetics2077.mixins.json' else []);target.write_text(json.dumps(data,indent=2))
    elif target.name=='pack.mcmeta':target.write_text('{"pack":{"pack_format":15,"description":"Cybernetics 2077"}}\n')
    elif path.suffix=='.json' and 'shaders' in parts and 'core' in parts:
        data=json.loads(path.read_text())
        if path.stem in ('hologram','temporal_ghost'):data['attributes']=['Position','Color','UV0','UV1','UV2','Normal']
        elif path.stem=='temporal_lens':data['attributes']=['Position','UV0']
        else:data['attributes']=['Position','UV0','Color']
        if path.stem!='temporal_lens':
            data['blend']={'func':'add','srcrgb':'one' if path.stem=='skin_composite' else 'srcalpha','dstrgb':'1-srcalpha','srcalpha':'one','dstalpha':'1-srcalpha'}
        target.write_text(json.dumps(data,indent=2)+'\n')
    elif path.suffix=='.json' and parts[0]=='data':
        data=json.loads(path.read_text())
        if 'recipes' in parts and 'result' in data and 'id' in data['result']:data['result']['item']=data['result'].pop('id')
        if 'advancements' in parts:
            for criterion in data.get('criteria',{}).values():
                for predicate in criterion.get('conditions',{}).get('items',[]):
                    if isinstance(predicate.get('items'),str):predicate['items']=[predicate['items']]
        target.write_text(json.dumps(data,indent=2)+'\n')
    elif path.suffix=='.nbt':
        data=gzip.decompress(path.read_bytes());marker=b'\x03\x00\x0bDataVersion';at=data.find(marker)
        if at>=0:data=data[:at+len(marker)]+struct.pack('>i',3465)+data[at+len(marker)+4:]
        target.write_bytes(gzip.compress(data,mtime=0))
    else:shutil.copyfile(path,target)
if '--tests' in sys.argv:
    target=OUT/'resources/data/cybernetics2077/structures/empty.nbt';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/'docs/review/resources/data/cybernetics2077/structure/empty.nbt',target)
print('Generated Forge source and resource adapters')
