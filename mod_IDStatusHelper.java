package net.minecraft.src;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;

import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.TextArea;

public class mod_IDStatusHelper extends BaseMod {

	private ModSettingScreen modScreen;
	private TextArea quickStatus;
	
	private int shiftAmount = 0;
	
	@Override
	public String getVersion() {
		return "1.2.3";
	}
	
	@SuppressWarnings("unused")
	private void saveIDStatusToFile() {
		File savePath = new File(Minecraft.getMinecraftDir(), "ID Status.txt");
		try {
			FileOutputStream output = new FileOutputStream(savePath);
			output.write(generateIDStatusReport().getBytes());
			output.flush();
			output.close();
			GuiModScreen.show(GuiApiHelper.makeTextDisplayAndGoBack(
					"ID Status Helper",
					"Saved ID status report to " + savePath.getAbsolutePath(),
					"OK", false));
		} catch (Throwable e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String trace = sw.toString();
			pw.close();
			GuiModScreen.show(GuiApiHelper.makeTextDisplayAndGoBack(
					"ID Status Helper",
					"Error saving to " + savePath.getAbsolutePath()
							+ ", exception was:\r\n\r\n" + trace, "OK", false));
		}
	}

	@Override
	public void load() {
		shiftAmount = Item.shovelSteel.shiftedIndex;
		modScreen = new ModSettingScreen("ID Status Report");
		modScreen.setSingleColumn(true);
		WidgetTick ticker = new WidgetTick();
		ticker.addTimedCallback(new ModAction(this, "onFirstView"), 1);
		modScreen.theWidget.add(ticker);
		quickStatus = GuiApiHelper.makeTextArea("Getting ID Status...", false);
		modScreen.widgetColumn.add(quickStatus);
		modScreen.widgetColumn.heightOverrideExceptions.put(quickStatus, 0);
		
		modScreen.widgetColumn.add(GuiApiHelper.makeButton("Generate ID Status Report",
				"saveIDStatusToFile", this, true));

		modScreen.widgetColumn.add(GuiApiHelper.makeButton("Display ID Status Report",
				"displayIDStatus", this, true));
		
	}
	
	@SuppressWarnings("unused")
	private void onFirstView()
	{
		boolean checkClean = Block.blocksList.length != shiftAmount;
		int totalRegisteredBlocks = 1;
		int totalUncleanBlockSlots = 0;
		int totalRegisteredItems = 0;
		
		for (int i = 1; i < Block.blocksList.length; i++) {
			if (Block.blocksList[i] != null) {
				totalRegisteredBlocks++;
			}
			else
			{
				if (checkClean && Item.itemsList[i] != null) {
					totalUncleanBlockSlots++;
				}
			}
		}
		
		for (int i = shiftAmount; i < Item.itemsList.length; i++) {
			if (Item.itemsList[i] != null) {
				if(i < Block.blocksList.length)
				{
					if(Block.blocksList[i] != null)
					{
						continue;
					}
				}
				totalRegisteredItems++;
			}
		}
		
		StringBuilder builder = new StringBuilder("Quick ID Status:\n");
		
		builder.append("Registered Blocks: ");
		builder.append(totalRegisteredBlocks);
		builder.append("\n");
		
		if(checkClean)
		{
			builder.append("Unclean Block slots: ");
			builder.append(totalUncleanBlockSlots);
			builder.append("\n");
		}
		
		builder.append("Available Block slots: ");
		builder.append(Block.blocksList.length - (totalRegisteredBlocks + totalUncleanBlockSlots));
		builder.append("\n");
		
		builder.append("Registered Items: ");
		builder.append(totalRegisteredItems);
		builder.append("\n");
		
		builder.append("Available Item Slots: ");
		builder.append((Item.itemsList.length - shiftAmount) - totalRegisteredItems);
		builder.append("\n");
		
		try {
			Field modLoaderSprites = ModLoader.class.getDeclaredField("itemSpritesLeft");
			modLoaderSprites.setAccessible(true);
			int remainingSprites = (Integer) modLoaderSprites.get(null);
			builder.append("Remaining Sprite Indexes: ");
			builder.append(remainingSprites);
		} catch (Throwable e) {
			builder.append("Remaining Sprite Indexes: (Could not retrive information)");
		}
		
		GuiApiHelper.setTextAreaText(quickStatus, builder.toString());
	}
	

	private String getItemNameForStack(ItemStack stack) {
		if(stack.getItem() == null)
		{
			return "";
		}
		return stack.getItem().getItemNameIS(stack);
	}
	
	
	@SuppressWarnings("unused")
	private void tickIDSubItem(WidgetItem2DRender renderer, TextArea textArea,Label label)
	{
		ItemStack stack = renderer.getRenderStack();
		int damage = stack.getItemDamage();
		damage++;
		if(damage > 15)
		{
			damage = 0;
		}
		stack.setItemDamage(damage);
		
		Item item = stack.getItem();
		String stackName = getItemNameForStack(stack);
		StringBuilder tooltipText = new StringBuilder(String.format(
				"Slot %-4s : Metadata %s: %s",
				stack.itemID,damage,
				StringTranslate.getInstance().translateNamedKey(
						stackName)));

		tooltipText.append(String.format("\r\n\r\nInternal name: %s",
				stackName));
		tooltipText.append(String.format("\r\nSubitems: %s",
				item.hasSubtypes));
		tooltipText.append(String.format("\r\nMax stack: %s",
				item.getItemStackLimit()));
		tooltipText.append(String.format(
				"\r\nDamage versus entities: %s",
				item.getDamageVsEntity(null)));
		tooltipText.append(String.format("\r\nEnchantability: %s",
				item.getItemEnchantability()));
		tooltipText.append(String.format("\r\nMax Damage: %s",
				item.getMaxDamage()));
		if (getExistance(getPosition(stack.itemID),stack.itemID) == 1) {
			tooltipText.append(String.format("\r\nBlock Hardness: %s",
					Block.blocksList[stack.itemID].getHardness()));
			tooltipText.append(String.format(
					"\r\nBlock Slipperiness: %s",
					Block.blocksList[stack.itemID].slipperiness));
			tooltipText.append(String.format(
					"\r\nBlock Light Level: %s",
					Block.lightValue[stack.itemID]));
			tooltipText.append(String.format(
					"\r\nBlock Opacity: %s",
					Block.lightOpacity[stack.itemID]));
		}
		
		GuiApiHelper.setTextAreaText(textArea, tooltipText.toString());
	}
	
	
	private int getPosition(int i)
	{
		int position = 0;
		if(i >= Block.blocksList.length)
		{
			position = 2;
		}
		else
		{
			if(i >= shiftAmount)
			{
				position = 1;
			}
		}
		return position;
	}
	
	private int getExistance(int position,int i)
	{
		ItemStack stack = new ItemStack(i, 1, 0);
		int exists = 0;
		switch(position)
		{
		case 0:
		{
			if((Block.blocksList[i] != null) && (stack != null) && (stack.getItem() != null))
			{
				exists = 1;
			}
			break;
		}
		case 1:
		{
			if((Block.blocksList[i] != null) && (stack != null) && (stack.getItem() != null))
			{
				exists = 1;
			}
			else
			{
				if((Item.itemsList[i] != null) && (stack != null))
				{
					exists = 2;
				}
			}
			break;
		}
		case 2:
		{
			if((Item.itemsList[i] != null) && (stack != null))
			{
				exists = 2;
			}
			break;
		}
		}
		return exists;
	}
	
	@SuppressWarnings("unused")
	private void displayIDStatus() {
		ModAction mergedActions = null;
		
		WidgetSinglecolumn area = new WidgetSinglecolumn();

		area.childDefaultWidth = 250;
		area.childDefaultHeight = 40;
		int freeSlotStart = -1;
		String[] freeName = new String[]{"block", "block or item","item"};
		String[] freeNames = new String[]{"blocks", "blocks or items","items"};
		for (int i = 1; i < Item.itemsList.length; i++) {
			
			
			boolean addTick = false;
			Label label = null;
			StringBuilder tooltipText = null;
			ItemStack stack = new ItemStack(i, 1, 0);
			int position = getPosition(i);
			int exists = getExistance(position,i);
			
			if (exists == 0) {
				if (freeSlotStart == -1) {
					freeSlotStart = i;
				}
				int next = i + 1;
				if(next != Item.itemsList.length)
				{
				int nextPosition = getPosition(next);
				int nextExists = getExistance(nextPosition,next);
				
				boolean generateRangeItem = (nextExists != 0);
				
				if (!generateRangeItem && (nextPosition == position)) {
					continue;
				}
				}
				
				
				
				if (freeSlotStart != i) {
					label = new Label(String.format(
							"Slots %-4s - %-4s: Open slots", freeSlotStart, i));
					tooltipText = new StringBuilder(
							String.format(
									"Open Slots\r\n\r\nThis slot range of %s is open for any %s to use.",
									i - freeSlotStart,
									freeNames[position]));
				} else {
					label = new Label(String.format("Slot %-4s: Open slot", i));
					tooltipText = new StringBuilder(
							String.format(
									"Open Slot\r\n\r\nThis slot is open for any %s to use.",
									freeName[position]));
				}
				freeSlotStart = -1;
			}
			else
			{
				String stackName = getItemNameForStack(stack);
				tooltipText = new StringBuilder(String.format(
						"Slot %-4s: %s",
						i,
						StringTranslate.getInstance().translateNamedKey(
								stackName)));
				label = new Label(tooltipText.toString());

				tooltipText.append(String.format("\r\n\r\nInternal name: %s",
						stackName));
				addTick = Item.itemsList[i].hasSubtypes;
				tooltipText.append(String.format("\r\nSubitems: %s",
						Item.itemsList[i].hasSubtypes));
				tooltipText.append(String.format("\r\nMax stack: %s",
						Item.itemsList[i].getItemStackLimit()));
				tooltipText.append(String.format(
						"\r\nDamage versus entities: %s",
						Item.itemsList[i].getDamageVsEntity(null)));
				tooltipText.append(String.format("\r\nEnchantability: %s",
						Item.itemsList[i].getItemEnchantability()));
				tooltipText.append(String.format("\r\nMax Damage: %s",
						Item.itemsList[i].getMaxDamage()));
				if (exists == 1) {
					tooltipText.append(String.format("\r\nBlock Hardness: %s",
							Block.blocksList[i].getHardness()));
					tooltipText.append(String.format(
							"\r\nBlock Slipperiness: %s",
							Block.blocksList[i].slipperiness));
					tooltipText.append(String.format(
							"\r\nBlock Light Level: %s",
							Block.lightValue[i]));
					tooltipText.append(String.format(
							"\r\nBlock Opacity: %s",
							Block.lightOpacity[i]));
				}
			}
			
			WidgetSingleRow row = new WidgetSingleRow(200, 32);
			WidgetItem2DRender renderer = new WidgetItem2DRender(i);
			row.add(renderer, 32, 32);
			TextArea tooltip = GuiApiHelper.makeTextArea(tooltipText.toString(), false);
			if(addTick)
			{
				ModAction action = new ModAction(this, "tickIDSubItem", WidgetItem2DRender.class, TextArea.class,Label.class).setDefaultArguments(renderer,tooltip,label);
				action.setTag("SubItem Tick for " + tooltipText.subSequence(0, tooltipText.indexOf("\r\n")));
				if(mergedActions != null)
				{
					mergedActions = mergedActions.mergeAction(action);
				}
				else
				{
					mergedActions = action;
				}
			}
			label.setTooltipContent(tooltip);
			row.add(label);
			area.add(row);
		}

		WidgetSimplewindow window = new WidgetSimplewindow(area,
				"ID Status Report");
		if(mergedActions != null)
		{
		WidgetTick ticker = new WidgetTick();
		ticker.addCallback(mergedActions, 500);
		window.mainWidget.add(ticker);
		}
		window.backButton.setText("OK");
		GuiModScreen.show(window);
	}

	
	@SuppressWarnings("unused")
	private String generateIDStatusReport() {
		StringBuilder report = new StringBuilder();
		String linebreak = System.getProperty("line.separator");
		report.append("ID Status report").append(linebreak);
		report.append("Generated on " + new Date().toString())
				.append(linebreak).append(linebreak);

		
		boolean checkClean = Block.blocksList.length != shiftAmount;
		int totalRegisteredBlocks = 1;
		int totalUncleanBlockSlots = 0;
		int totalRegisteredItems = 0;
		
		StringBuilder reportIDs = new StringBuilder();
		for (int i = 1; i < Block.blocksList.length; i++) {
			Block block = Block.blocksList[i];
			if (block == null) {
				if (checkClean && Item.itemsList[i] != null) {
					totalUncleanBlockSlots++;
				}
				continue;
			}
			totalRegisteredBlocks++;
				
			String blockName = block.getBlockName();
			String transName = StatCollector.translateToLocal(blockName
					+ ".name");
			reportIDs.append(
					String.format("%-8s - %-31s - %-31s - %s", i, blockName,
							transName, block.getClass().getName())).append(
					linebreak);
		}

		for (int i = shiftAmount; i < Item.itemsList.length; i++) {
			Item item = Item.itemsList[i];
			if (item == null) {
				continue;
			}
			if(i < Block.blocksList.length)
			{
				if(Block.blocksList[i] != null)
				{
					continue;
				}
			}
			totalRegisteredItems++;
			String itemName = item.getItemName();
			String transName = StatCollector.translateToLocal(itemName
					+ ".name");
			reportIDs.append(
					String.format("%-8s - %-31s - %-31s - %s", i, itemName,
							transName, item.getClass().getName())).append(
					linebreak);
		}
		report.append("Quick stats:").append(linebreak);
		report.append(
				String.format("Block ID Status: %d/%d used. %d available.",
						totalRegisteredBlocks, Block.blocksList.length,
						(Block.blocksList.length - totalUncleanBlockSlots) - totalRegisteredBlocks));
		if(checkClean)
		{
			report.append("(Unclean Block slots: ");
			report.append(totalUncleanBlockSlots);
			report.append(")" + linebreak);
		}
		else
		{
			report.append(linebreak);
		}
		report.append(
				String.format("Item ID Status: %d/%d used. %d available.",
						totalRegisteredItems, Item.itemsList.length,
						(Item.itemsList.length - shiftAmount) - totalRegisteredItems)).append(linebreak)
				.append(linebreak);
		report.append(
				"ID      - Name                           - Tooltip                        - Class")
				.append(linebreak);
		report.append(reportIDs.toString());
		return report.toString();
	}
	
	
}
