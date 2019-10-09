/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Arduino LLC (http://www.arduino.cc/)
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */
package cc.arduino.contributions.ui;

import cc.arduino.contributions.libraries.ContributedLibraryReleases;
import cc.arduino.contributions.libraries.LibraryInstaller;
import cc.arduino.contributions.libraries.LibraryTypeComparator;
import cc.arduino.contributions.libraries.ui.*;
import cc.arduino.contributions.packages.ui.ContributionIndexTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.app.BaseNoGui;
import processing.app.I18n;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static processing.app.I18n.tr;
import static processing.app.Theme.scale;

public class ManagerDialog extends JDialog {
  private static Logger log = LogManager.getLogger(ManagerDialog.class);

  ////////////////
  //
  // Constructors
  //
  ////////////////

  public ManagerDialog(Window parent, DialogType dialogType) {
    super (parent);
    _dialogType = dialogType;

    initData();
    initWindow();
    initComponents();
  }

  ////////////////
  //
  // Public interface
  //
  ////////////////
  public enum DialogType { LIBRARY, BOARD }

  public void updateUI() {
  }

  ////////////////
  //
  // Private interface data
  //
  ////////////////
  // Real contribution table
  protected JTable _contribTable = null;
  // Model behind the table
//  protected final FilteredAbstractTableModel<T> contribModel;
  protected FilteredAbstractTableModel _contribModel = null;

  private DialogType _dialogType = null;
  private JButton    _okButton = null;

  private JPanel     _filtersContainer = null;
  private JComboBox  _categoryChooser = null;
  private JLabel     _categoryChooserLbl = null;
  private JComboBox  _topicChooser = null;
  private JLabel     _topicChooserLbl = null;

  private JPanel     _nameContainer = null;
  private JComboBox  _nameChooser = null;
  private JLabel     _nameChooserLbl = null;

  ////////////////
  //
  // Private interface
  //
  ////////////////
  private void initData() {
    log.info("About to gather the data");
    switch (_dialogType) {
      case LIBRARY:
        _contribModel = new LibrariesIndexTableModel();
        break;
      case BOARD:
        _contribModel = new ContributionIndexTableModel();
        break;
    }
    log.info("Gathered the data, make a table");

    _contribTable = new JTable(_contribModel);

    log.debug(_contribTable);
  }

  private void initWindow() {
    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    String title = "";

    switch (_dialogType) {
      case LIBRARY:
        title = tr("Library Manager");
        break;
      case BOARD:
        title = tr("Board Manager");
        break;
    }

    setTitle(title);
    setModal(true);
    setResizable(true);  // change to false if appropriate

    setMinimumSize(scale(new Dimension(800, 450)));
  }

  private void initComponents() {
    Container pane = getContentPane();
    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

    setupFilterPanel(pane);
    setupNamePanel(pane);

    setupOKButton();
    add(_okButton);
  }

  ////////////////////////////////
  //
  // Filter Panel
  //
  ////////////////////////////////
  private void setupFilterPanel(Container pane) {
    _filtersContainer = new JPanel();
    _filtersContainer.setLayout(new BoxLayout(_filtersContainer, BoxLayout.X_AXIS));

    setupCategoryChooser();
    _filtersContainer.add(Box.createHorizontalStrut(5));
    _filtersContainer.add(_categoryChooserLbl);
    _filtersContainer.add(Box.createHorizontalStrut(5));
    _filtersContainer.add(_categoryChooser);

    if (_dialogType == DialogType.LIBRARY) {
      setupTopicChooser();
      _filtersContainer.add(Box.createHorizontalStrut(5));
      _filtersContainer.add(_topicChooserLbl);
      _filtersContainer.add(Box.createHorizontalStrut(5));
      _filtersContainer.add(_topicChooser);
    }

    _filtersContainer.setBorder(new EmptyBorder(7, 7, 7, 7));
    pane.add(_filtersContainer, BorderLayout.NORTH);
  }

  private void setupCategoryChooser() {
    String label = tr("Type");

    // Create the combo box and it's label
    _categoryChooserLbl = new JLabel(label);
    _categoryChooser = new JComboBox();

    if (_categoryChooser != null) { // just to make sure it got allocated!
      // set dimensions
      _categoryChooser.setMaximumRowCount(20);
      _categoryChooser.getAccessibleContext().setAccessibleDescription(label);
      _categoryChooser.setVisible(true);

      // Populate the combo box
      _categoryChooser.removeAllItems();
      _categoryChooser.addItem(new DropdownAllLibraries());
      Collection<String> categories = null;
      switch (_dialogType) {
        case LIBRARY:
          _categoryChooser.addItem(new DropdownUpdatableLibrariesItem());
          _categoryChooser.addItem(new DropdownInstalledLibraryItem());
          BaseNoGui.librariesIndexer.getIndex().getTypes();
          categories = BaseNoGui.librariesIndexer.getIndex().getTypes();
          break;
        case BOARD:
          categories = BaseNoGui.indexer.getCategories();
          break;

        default:
          // log error if needed
          break;
      }
      for (String category : categories) {
        _categoryChooser.addItem(new DropdownLibraryOfCategoryItem(category));
      }
      _categoryChooser.setEnabled(_categoryChooser.getItemCount() > 1);

      // set listener - this will cause other lists to change
      _categoryChooser.addActionListener(categoryChooserActionListener);
    } // allocated
  }

  protected final ActionListener categoryChooserActionListener = new ActionListener() {

    @Override
    public void actionPerformed(ActionEvent event) {
    }
  };

  private void setupTopicChooser() {
    String label = tr("Topic");

    // Create topic box and label
    _topicChooserLbl = new JLabel(label);
    _topicChooser = new JComboBox();

    if (_topicChooser != null) { // just to make sure it got allocated
      // set dimentions
      _topicChooser.setMaximumRowCount(20);
      _topicChooser.getAccessibleContext().setAccessibleDescription(label);
      _topicChooser.setVisible(true);

      // Populate the combo box
      _topicChooser.removeAllItems();
      _topicChooser.addItem(new DropdownAllLibraries());
      List<String> topics = null;
      switch (_dialogType) {
        case LIBRARY:
          topics = new LinkedList<>(BaseNoGui.librariesIndexer.getIndex().getCategories());
          Collections.sort(topics, new LibraryTypeComparator());
          break;
        default:
          // log error if needed
          break;
      }
      for (String topic : topics) {
        _topicChooser.addItem(new DropdownLibraryOfCategoryItem(topic));
      }
      _topicChooser.setEnabled(_categoryChooser.getItemCount() > 1);

      // set listener - this will cause other lists to change
      _topicChooser.addActionListener(topicChooserActionListener);
    } // allocated
  }

  protected final ActionListener topicChooserActionListener = new ActionListener() {

    @Override
    public void actionPerformed(ActionEvent event) {
    }
  };

  ////////////////////////////////
  //
  // Name Panel
  //
  ////////////////////////////////
  private void setupNamePanel(Container pane) {
    _nameContainer = new JPanel();
    _nameContainer.setLayout(new BoxLayout(_nameContainer,BoxLayout.X_AXIS));

    setupNameChooser();
    _nameContainer.add(Box.createHorizontalStrut(5));
    _nameContainer.add(_nameChooserLbl);
    _nameContainer.add(Box.createHorizontalStrut(5));
    _nameContainer.add(_nameChooser);

    _nameContainer.setBorder(new EmptyBorder(7, 7, 7, 7));
    pane.add(_nameContainer);
  }

  private void setupNameChooser() {
    String label = tr("Name");

    // Create the combo box and it's label
    _nameChooserLbl = new JLabel(label);
    _nameChooser = new JComboBox();

    if (_nameChooser != null) { // just to make sure it got allocated!
      // set dimensions
      _nameChooser.setMaximumRowCount(20);
      _nameChooser.getAccessibleContext().setAccessibleDescription(label);
      _nameChooser.setVisible(true);

      // Populate the combo box
      _nameChooser.removeAllItems();

      if (_contribTable != null) {
        for (int index = 0; index < _contribTable.getColumnCount(); index++) {
          log.error("_contribTable column {} is {}", index + 1, _contribTable.getColumnName(index));
          String string = index + " of " + _contribTable.getColumnCount() + " - " + _contribTable.getColumnName(index);
          _nameChooser.addItem(new DropdownLibraryOfCategoryItem(string));
        }
        _nameChooser.addItem(new DropdownLibraryOfCategoryItem(_contribTable.getRowCount() + " rows"));
      }
      else {
        _nameChooser.addItem(new DropdownLibraryOfCategoryItem("No data"));
      }
//      _nameChooser.addItem("_contribModel has " + _contribModel.getRowCount() + " rows");
//      for (int i = 0; i < _contribModel.getRowCount(); i++) {
//        Object model = _contribModel.getValueAt(i,0);
//        _nameChooser.addItem(new DropdownLibraryOfCategoryItem(i + " of " + _contribModel.getRowCount() + ": " + model.toString()));
//      }
      _nameChooser.setEnabled(_nameChooser.getItemCount() > 1);

      // set listener - this will cause other lists to change
      _nameChooser.addActionListener(categoryChooserActionListener);
    } // allocated
  }

  private void setupOKButton() {
    _okButton = new JButton();
    _okButton.setText(I18n.PROMPT_OK);
    _okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        okButtonActionPerformed(evt);
      }
    });
  }

  private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
    // do OK stuff here
    cancelButtonActionPerformed(evt);
  }

  private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
  }

}
