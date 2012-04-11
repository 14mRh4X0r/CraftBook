// $Id$
/*
 * CraftBook
 * Copyright (C) 2010 Lymia <lymiahugs@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

import lymia.plc.PlcBase;
import lymia.plc.PlcException;
import lymia.plc.PlcLang;
import com.sk89q.craftbook.BlockType;
import com.sk89q.craftbook.CraftBookWorld;
import com.sk89q.craftbook.SignText;
import com.sk89q.craftbook.Vector;

/**
 * A PLC that gets code from code block signs.
 * 
 * @author Lymia
 */
public class DefaultPLC extends PlcBase {
    /**
     * Construct the instance.
     * 
     * @param language
     */
    public DefaultPLC(PlcLang language) {
        super(language);
    }

    /**
     * Get the title of the IC.
     *
     * @return
     */
    public String getTitle() {
        return getLanguage().getName() + " PLC";
    }

    /**
     * Validates the IC's environment. The position of the sign is given.
     * Return a string in order to state an error message and deny
     * creation, otherwise return null to allow.
     *
     * @param sign
     * @return
     */
    protected String validateEnviromentEx(CraftBookWorld cbworld, Vector v, SignText t) {
        try {
            return getLanguage().validateEnvironment(cbworld, v, t, getCode(cbworld, v));
        } catch (PlcException e) {
            return e.toString();
        }
    }

    /**
     * Get the code.
     * 
     * @param v
     */
    protected String getCode(CraftBookWorld cbworld, Vector v) throws PlcException {
        //Server s = etc.getServer();
    	World world = CraftBook.getWorld(cbworld);
    	
        StringBuilder b = new StringBuilder();
        int x = v.getBlockX();
        int z = v.getBlockZ();
        int x0 = x;
        int y0 = v.getBlockY();
        int z0 = z;
        for (int y = 0; y < CraftBook.MAP_BLOCK_HEIGHT; y++)
            if (CraftBook.getBlockID(world, x, y, z) == BlockType.WALL_SIGN) {
                if (((Sign) world.getComplexBlock(x, y, z)).getText(1)
                        .equalsIgnoreCase("[CODE BLOCK]"))
                    for (y--; y >= 0; y--)
                        if (!(x == x0 && y == y0 && z == z0)
                                && CraftBook.getBlockID(world, x, y, z) == BlockType.WALL_SIGN) {
                            Sign n = (Sign) world.getComplexBlock(x, y, z);
                            b.append(n.getText(0) + "\n");
                            b.append(n.getText(1) + "\n");
                            b.append(n.getText(2) + "\n");
                            b.append(n.getText(3) + "\n");
                        } else
                            return b.toString();
            }
        throw new PlcException("code not found");
    }
}
