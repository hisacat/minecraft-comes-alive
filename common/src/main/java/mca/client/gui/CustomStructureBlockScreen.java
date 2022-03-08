package mca.client.gui;

import mca.block.StructureBlockEntity;
import mca.cobalt.network.NetworkHandler;
import mca.network.client.FamilyTreeUUIDResponse;
import mca.network.client.StructureBlockUpdateMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.glfw.GLFW;

@Environment(value = EnvType.CLIENT)
public class CustomStructureBlockScreen extends Screen {
    private static final Text STRUCTURE_NAME_TEXT = new TranslatableText("structure_block.structure_name");
    private static final Text POSITION_TEXT = new TranslatableText("structure_block.position");
    private static final Text SIZE_TEXT = new TranslatableText("structure_block.size");
    private static final Text DETECT_SIZE_TEXT = new TranslatableText("structure_block.detect_size");
    private static final Text SHOW_AIR_TEXT = new TranslatableText("structure_block.show_air");
    private static final Text SHOW_BOUNDING_BOX_TEXT = new TranslatableText("structure_block.show_boundingbox");

    private final StructureBlockEntity structureBlock;
    private BlockMirror mirror = BlockMirror.NONE;
    private BlockRotation rotation = BlockRotation.NONE;
    private StructureBlockMode mode = StructureBlockMode.LOAD;
    private boolean showAir;
    private boolean showBoundingBox;

    private TextFieldWidget inputName;
    private TextFieldWidget inputPosX;
    private TextFieldWidget inputPosY;
    private TextFieldWidget inputPosZ;
    private TextFieldWidget inputSizeX;
    private TextFieldWidget inputSizeY;
    private TextFieldWidget inputSizeZ;
    private ButtonWidget buttonSave;
    private ButtonWidget buttonLoad;
    private ButtonWidget buttonRotate0;
    private ButtonWidget buttonRotate90;
    private ButtonWidget buttonRotate180;
    private ButtonWidget buttonRotate270;
    private ButtonWidget buttonDetect;
    private CyclingButtonWidget<BlockMirror> buttonMirror;
    private CyclingButtonWidget<Boolean> buttonShowAir;
    private CyclingButtonWidget<Boolean> buttonShowBoundingBox;

    public CustomStructureBlockScreen(StructureBlockEntity structureBlock) {
        super(new TranslatableText(Blocks.STRUCTURE_BLOCK.getTranslationKey()));
        this.structureBlock = structureBlock;
    }

    @Override
    public void tick() {
        inputName.tick();
        inputPosX.tick();
        inputPosY.tick();
        inputPosZ.tick();
        inputSizeX.tick();
        inputSizeY.tick();
        inputSizeZ.tick();
    }

    private void done() {
        updateStructureBlock(StructureBlockEntity.Action.UPDATE_DATA);
        client.setScreen(null);
    }

    private void cancel() {
        structureBlock.setMirror(mirror);
        structureBlock.setRotation(rotation);
        structureBlock.setMode(mode);
        structureBlock.setShowAir(showAir);
        structureBlock.setShowBoundingBox(showBoundingBox);
        client.setScreen(null);
    }

    @Override
    protected void init() {
        client.keyboard.setRepeatEvents(true);
        addDrawableChild(new ButtonWidget(width / 2 - 4 - 150, 210, 150, 20, ScreenTexts.DONE, button -> done()));
        addDrawableChild(new ButtonWidget(width / 2 + 4, 210, 150, 20, ScreenTexts.CANCEL, button -> cancel()));

        mirror = structureBlock.getMirror();
        rotation = structureBlock.getRotation();
        mode = structureBlock.getMode();
        showAir = structureBlock.shouldShowAir();
        showBoundingBox = structureBlock.shouldShowBoundingBox();

        buttonSave = addDrawableChild(new ButtonWidget(width / 2 + 4 + 50, 185, 100, 20, new TranslatableText("structure_block.button.save"), button -> {
            if (structureBlock.getMode() == StructureBlockMode.SAVE) {
                updateStructureBlock(StructureBlockEntity.Action.SAVE_AREA);
                client.setScreen(null);
            }
        }));

        buttonLoad = addDrawableChild(new ButtonWidget(width / 2 + 4 + 50, 185, 100, 20, new TranslatableText("structure_block.button.load"), button -> {
            if (structureBlock.getMode() == StructureBlockMode.LOAD) {
                updateStructureBlock(StructureBlockEntity.Action.LOAD_AREA);
                client.setScreen(null);
            }
        }));

        addDrawableChild(CyclingButtonWidget.builder(value -> new TranslatableText("structure_block.mode." + ((StructureBlockMode)value).asString()))
                .values(StructureBlockMode.SAVE, StructureBlockMode.LOAD, StructureBlockMode.CORNER)
                .omitKeyText()
                .initially(mode)
                .build(width / 2 - 4 - 150, 185, 100, 20, new LiteralText("MODE"), (button, mode) -> {
                    structureBlock.setMode((StructureBlockMode)mode);
                    updateWidgets((StructureBlockMode)mode);
                }));

        buttonDetect = addDrawableChild(new ButtonWidget(width / 2 + 4 + 50, 120, 100, 20, new TranslatableText("structure_block.button.detect_size"), button -> {
            if (structureBlock.getMode() == StructureBlockMode.SAVE) {
                updateStructureBlock(StructureBlockEntity.Action.SCAN_AREA);
                client.setScreen(null);
            }
        }));

        buttonShowAir = addDrawableChild(CyclingButtonWidget.onOffBuilder(structureBlock.shouldShowAir())
                .omitKeyText().build(width / 2 + 4 + 50, 80, 100, 20, SHOW_AIR_TEXT,
                        (button, showAir) -> structureBlock.setShowAir(showAir)));

        buttonShowBoundingBox = addDrawableChild(CyclingButtonWidget.onOffBuilder(structureBlock.shouldShowBoundingBox())
                .omitKeyText().build(width / 2 + 4 + 50, 80, 100, 20, SHOW_BOUNDING_BOX_TEXT,
                        (button, showBoundingBox) -> structureBlock.setShowBoundingBox(showBoundingBox)));

        buttonMirror = addDrawableChild(CyclingButtonWidget.builder(BlockMirror::getName)
                .values(BlockMirror.values()).omitKeyText()
                .initially(mirror).build(width / 2 - 20, 120, 40, 20, new LiteralText("MIRROR"),
                        (button, mirror) -> structureBlock.setMirror(mirror)));

        buttonRotate0 = addDrawableChild(new ButtonWidget(width / 2 - 1 - 40 - 1 - 40 - 20, 120, 40, 20, new LiteralText("0"), button -> {
            structureBlock.setRotation(BlockRotation.NONE);
            updateRotationButton();
        }));
        buttonRotate90 = addDrawableChild(new ButtonWidget(width / 2 - 1 - 40 - 20, 120, 40, 20, new LiteralText("90"), button -> {
            structureBlock.setRotation(BlockRotation.CLOCKWISE_90);
            updateRotationButton();
        }));
        buttonRotate180 = addDrawableChild(new ButtonWidget(width / 2 + 1 + 20, 120, 40, 20, new LiteralText("180"), button -> {
            structureBlock.setRotation(BlockRotation.CLOCKWISE_180);
            updateRotationButton();
        }));
        buttonRotate270 = addDrawableChild(new ButtonWidget(width / 2 + 1 + 40 + 1 + 20, 120, 40, 20, new LiteralText("270"), button -> {
            structureBlock.setRotation(BlockRotation.COUNTERCLOCKWISE_90);
            updateRotationButton();
        }));

        inputName = new TextFieldWidget(textRenderer, width / 2 - 152, 40, 300, 20, new TranslatableText("structure_block.structure_name")) {
            @Override
            public boolean charTyped(char chr, int modifiers) {
                if (!isValidCharacterForName(getText(), chr, getCursor())) {
                    return false;
                }
                return super.charTyped(chr, modifiers);
            }
        };
        inputName.setMaxLength(64);
        inputName.setText(structureBlock.getStructureName());
        addSelectableChild(inputName);

        BlockPos blockPos = structureBlock.getOffset();
        inputPosX = new TextFieldWidget(textRenderer, width / 2 - 152, 80, 50, 20, new TranslatableText("structure_block.position.x"));
        inputPosX.setMaxLength(15);
        inputPosX.setText(Integer.toString(blockPos.getX()));
        addSelectableChild(inputPosX);
        inputPosY = new TextFieldWidget(textRenderer, width / 2 - 102, 80, 50, 20, new TranslatableText("structure_block.position.y"));
        inputPosY.setMaxLength(15);
        inputPosY.setText(Integer.toString(blockPos.getY()));
        addSelectableChild(inputPosY);
        inputPosZ = new TextFieldWidget(textRenderer, width / 2 - 52, 80, 50, 20, new TranslatableText("structure_block.position.z"));
        inputPosZ.setMaxLength(15);
        inputPosZ.setText(Integer.toString(blockPos.getZ()));
        addSelectableChild(inputPosZ);

        Vec3i vec3i = structureBlock.getSize();
        inputSizeX = new TextFieldWidget(textRenderer, width / 2 - 152, 120, 50, 20, new TranslatableText("structure_block.size.x"));
        inputSizeX.setMaxLength(15);
        inputSizeX.setText(Integer.toString(vec3i.getX()));
        addSelectableChild(inputSizeX);
        inputSizeY = new TextFieldWidget(textRenderer, width / 2 - 102, 120, 50, 20, new TranslatableText("structure_block.size.y"));
        inputSizeY.setMaxLength(15);
        inputSizeY.setText(Integer.toString(vec3i.getY()));
        addSelectableChild(inputSizeY);
        inputSizeZ = new TextFieldWidget(textRenderer, width / 2 - 52, 120, 50, 20, new TranslatableText("structure_block.size.z"));
        inputSizeZ.setMaxLength(15);
        inputSizeZ.setText(Integer.toString(vec3i.getZ()));
        addSelectableChild(inputSizeZ);

        updateRotationButton();
        updateWidgets(mode);
        setInitialFocus(inputName);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String string = inputName.getText();
        String string2 = inputPosX.getText();
        String string3 = inputPosY.getText();
        String string4 = inputPosZ.getText();
        String string5 = inputSizeX.getText();
        String string6 = inputSizeY.getText();
        String string7 = inputSizeZ.getText();
        init(client, width, height);
        inputName.setText(string);
        inputPosX.setText(string2);
        inputPosY.setText(string3);
        inputPosZ.setText(string4);
        inputSizeX.setText(string5);
        inputSizeY.setText(string6);
        inputSizeZ.setText(string7);
    }

    @Override
    public void removed() {
        client.keyboard.setRepeatEvents(false);
    }

    private void updateRotationButton() {
        buttonRotate0.active = true;
        buttonRotate90.active = true;
        buttonRotate180.active = true;
        buttonRotate270.active = true;
        switch (structureBlock.getRotation()) {
            case NONE -> {
                buttonRotate0.active = false;
            }
            case CLOCKWISE_180 -> {
                buttonRotate180.active = false;
            }
            case COUNTERCLOCKWISE_90 -> {
                buttonRotate270.active = false;
            }
            case CLOCKWISE_90 -> {
                buttonRotate90.active = false;
            }
        }
    }

    private void updateWidgets(StructureBlockMode mode) {
        inputName.setVisible(false);
        inputPosX.setVisible(false);
        inputPosY.setVisible(false);
        inputPosZ.setVisible(false);
        inputSizeX.setVisible(false);
        inputSizeY.setVisible(false);
        inputSizeZ.setVisible(false);
        buttonSave.visible = false;
        buttonLoad.visible = false;
        buttonDetect.visible = false;
        buttonMirror.visible = false;
        buttonRotate0.visible = false;
        buttonRotate90.visible = false;
        buttonRotate180.visible = false;
        buttonRotate270.visible = false;
        buttonShowAir.visible = false;
        buttonShowBoundingBox.visible = false;
        switch (mode) {
            case SAVE -> {
                inputName.setVisible(true);
                inputPosX.setVisible(true);
                inputPosY.setVisible(true);
                inputPosZ.setVisible(true);
                inputSizeX.setVisible(true);
                inputSizeY.setVisible(true);
                inputSizeZ.setVisible(true);
                buttonSave.visible = true;
                buttonDetect.visible = true;
                buttonShowAir.visible = true;
            }
            case LOAD -> {
                inputName.setVisible(true);
                inputPosX.setVisible(true);
                inputPosY.setVisible(true);
                inputPosZ.setVisible(true);
                buttonLoad.visible = true;
                buttonMirror.visible = true;
                buttonRotate0.visible = true;
                buttonRotate90.visible = true;
                buttonRotate180.visible = true;
                buttonRotate270.visible = true;
                buttonShowBoundingBox.visible = true;
                updateRotationButton();
            }
            case CORNER -> {
                inputName.setVisible(true);
            }
        }
    }

    private void updateStructureBlock(StructureBlockEntity.Action action) {
        BlockPos blockPos = new BlockPos(parseInt(inputPosX.getText()), parseInt(inputPosY.getText()), parseInt(inputPosZ.getText()));
        Vec3i vec3i = new Vec3i(parseInt(inputSizeX.getText()), parseInt(inputSizeY.getText()), parseInt(inputSizeZ.getText()));
        structureBlock.setSize(vec3i);
        structureBlock.setOffset(blockPos);
        NetworkHandler.sendToServer(new StructureBlockUpdateMessage(structureBlock.getPos(), action, structureBlock.createNbt()));
    }

    private int parseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    @Override
    public void close() {
        cancel();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            done();
            return true;
        }
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        StructureBlockMode structureBlockMode = structureBlock.getMode();
        drawCenteredText(matrices, textRenderer, title, width / 2, 10, 0xFFFFFF);

        drawTextWithShadow(matrices, textRenderer, STRUCTURE_NAME_TEXT, width / 2 - 153, 30, 0xA0A0A0);
        inputName.render(matrices, mouseX, mouseY, delta);

        if (structureBlockMode == StructureBlockMode.LOAD || structureBlockMode == StructureBlockMode.SAVE) {
            drawTextWithShadow(matrices, textRenderer, POSITION_TEXT, width / 2 - 153, 70, 0xA0A0A0);
            inputPosX.render(matrices, mouseX, mouseY, delta);
            inputPosY.render(matrices, mouseX, mouseY, delta);
            inputPosZ.render(matrices, mouseX, mouseY, delta);
        }

        if (structureBlockMode == StructureBlockMode.SAVE) {
            drawTextWithShadow(matrices, textRenderer, SIZE_TEXT, width / 2 - 153, 110, 0xA0A0A0);
            inputSizeX.render(matrices, mouseX, mouseY, delta);
            inputSizeY.render(matrices, mouseX, mouseY, delta);
            inputSizeZ.render(matrices, mouseX, mouseY, delta);
            drawTextWithShadow(matrices, textRenderer, DETECT_SIZE_TEXT, width / 2 + 154 - textRenderer.getWidth(DETECT_SIZE_TEXT), 110, 0xA0A0A0);
            drawTextWithShadow(matrices, textRenderer, SHOW_AIR_TEXT, width / 2 + 154 - textRenderer.getWidth(SHOW_AIR_TEXT), 70, 0xA0A0A0);
        }

        if (structureBlockMode == StructureBlockMode.LOAD) {
            drawTextWithShadow(matrices, textRenderer, SHOW_BOUNDING_BOX_TEXT, width / 2 + 154 - textRenderer.getWidth(SHOW_BOUNDING_BOX_TEXT), 70, 0xA0A0A0);
        }

        drawTextWithShadow(matrices, textRenderer, structureBlockMode.asText(), width / 2 - 153, 174, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

